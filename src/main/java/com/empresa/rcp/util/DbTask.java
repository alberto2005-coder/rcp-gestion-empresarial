package com.empresa.rcp.util;

import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DbTask<T> extends Task<T> {
    private static final Logger logger = LoggerFactory.getLogger(DbTask.class);

    @Override
    protected abstract T call() throws Exception;

    @Override
    protected void succeeded() {
        super.succeeded();
        logger.info("DbTask completed successfully");
    }

    @Override
    protected void failed() {
        super.failed();
        Throwable e = getException();
        logger.error("DbTask failed with exception: ", e);
    }

    public void run() {
        Thread thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }
}
