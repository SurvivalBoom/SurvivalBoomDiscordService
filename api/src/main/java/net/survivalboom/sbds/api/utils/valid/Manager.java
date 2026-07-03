package net.survivalboom.sbds.api.utils.valid;

public abstract class Manager extends Valid implements IManager {

    public Manager() {
        setValid(false);
    }

    public void init() {

        if (isValid()) {
            throw new IllegalStateException("Manager already initialized");
        }

        setValid(true);

        try {
            init0();
        }

        catch (RuntimeException e) {
            setValid(false);
            throw e;
        }

    }

    public void shutdown() {

        checkValid();

        try {
            shutdown0();
        }

        finally {
            setValid(false);
        }

    }


    public boolean initIfNeeded() {

        if (isValid()) {
            return false;
        }

        init();

        return true;

    }

    public boolean shutdownIfNeeded() {

        if (!isValid()) {
            return false;
        }

        shutdown();

        return true;

    }


    protected abstract void init0();

    protected abstract void shutdown0();

}
