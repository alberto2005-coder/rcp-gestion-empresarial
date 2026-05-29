package com.empresa.rcp.sync;

public interface SyncProvider {
    boolean sendRecord(String table, String operation, int recordId, String data) throws Exception;
}
