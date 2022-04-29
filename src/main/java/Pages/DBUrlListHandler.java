package Pages;

import Database.IdbManager;

public class DBUrlListHandler implements IUrlListHandler {
    private IdbManager Manager;

    public DBUrlListHandler(IdbManager manager) {
        Manager = manager;
    }

    @Override
    public void add(String Url) {
        Manager.saveUrl(Url, 0);
    }

    @Override
    public boolean contains(String Url) {
        return Manager.searchUrl(Url);
    }
}
