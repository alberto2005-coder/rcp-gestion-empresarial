package com.empresa.rcp.sync;

import com.empresa.rcp.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SyncManager {
    private static final Logger logger = LoggerFactory.getLogger(SyncManager.class);
    private static SyncManager instance;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "SyncManager-Thread");
        thread.setDaemon(true);
        return thread;
    });

    private SyncProvider syncProvider;
    private boolean isSyncing = false;

    private SyncManager() {
        this.syncProvider = new DefaultSyncProvider();
        // Check for sync every 30 seconds
        scheduler.scheduleWithFixedDelay(this::performSync, 10, 30, TimeUnit.SECONDS);
    }

    public static synchronized SyncManager getInstance() {
        if (instance == null) {
            instance = new SyncManager();
        }
        return instance;
    }

    public void setSyncProvider(SyncProvider provider) {
        this.syncProvider = provider;
    }

    public void queueSync(String tabla, String operacion, int registroId, String datos) {
        String sql = "INSERT INTO sync_queue (tabla, operacion, registro_id, datos) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tabla);
            ps.setString(2, operacion);
            ps.setInt(3, registroId);
            ps.setString(4, datos);
            ps.executeUpdate();
            logger.info("Enqueued sync task: {} on table {} for ID {}", operacion, tabla, registroId);
            
            // Trigger sync execution in executor thread pool
            scheduler.execute(this::performSync);
        } catch (Exception e) {
            logger.error("Failed to queue sync record", e);
        }
    }

    public synchronized void performSync() {
        if (isSyncing) {
            return;
        }
        isSyncing = true;
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String countSql = "SELECT COUNT(*) FROM sync_queue";
            try (PreparedStatement ps = conn.prepareStatement(countSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) == 0) {
                    isSyncing = false;
                    return;
                }
            }
            
            logger.info("Starting background synchronization cycle...");
            
            if (!checkConnection()) {
                logger.warn("Offline: Synchronization delayed due to network unavailability");
                isSyncing = false;
                return;
            }

            String selectSql = "SELECT id, tabla, operacion, registro_id, datos FROM sync_queue ORDER BY id ASC LIMIT 50";
            boolean success = true;
            
            try (PreparedStatement ps = conn.prepareStatement(selectSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int syncId = rs.getInt("id");
                    String tabla = rs.getString("tabla");
                    String operacion = rs.getString("operacion");
                    int registroId = rs.getInt("registro_id");
                    String datos = rs.getString("datos");
                    
                    try {
                        boolean providerAck = syncProvider.sendRecord(tabla, operacion, registroId, datos);
                        if (providerAck) {
                            String deleteSql = "DELETE FROM sync_queue WHERE id = ?";
                            try (PreparedStatement deletePs = conn.prepareStatement(deleteSql)) {
                                deletePs.setInt(1, syncId);
                                deletePs.executeUpdate();
                            }
                            logger.info("Successfully synchronized transaction ID {}", syncId);
                        } else {
                            success = false;
                            break;
                        }
                    } catch (Exception e) {
                        logger.error("Error synchronizing transaction ID " + syncId, e);
                        success = false;
                        break;
                    }
                }
            }
            
            if (success) {
                logger.info("Synchronization cycle completed successfully.");
            }
        } catch (Exception e) {
            logger.error("Error during sync cycle", e);
        } finally {
            isSyncing = false;
        }
    }

    private boolean checkConnection() {
        try {
            java.net.URL url = java.net.URI.create("https://www.google.com").toURL();
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            conn.connect();
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private static class DefaultSyncProvider implements SyncProvider {
        @Override
        public boolean sendRecord(String table, String operation, int recordId, String data) throws Exception {
            logger.info("[Mock Cloud Sync] Sending table={} operation={} recordId={} data={}", 
                        table, operation, recordId, data);
            return true;
        }
    }
}
