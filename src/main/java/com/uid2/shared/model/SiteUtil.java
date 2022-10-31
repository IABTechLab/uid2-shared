package com.uid2.shared.model;

import com.uid2.shared.Const;

public class SiteUtil {
    public static boolean isValidSiteId(int siteId) {
        return siteId > Const.Data.AdvertisingTokenSiteId;
    }
}
