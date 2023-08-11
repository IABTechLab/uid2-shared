package com.uid2.shared.store;

import com.uid2.shared.model.Site;

import java.util.Collection;

public interface ISiteStore {
    Collection<Site> getAllSites();
    Site getSite(int siteId);
}
