package com.uid2.shared.secure;

import java.util.Collection;

public interface IEnclaveIdSet<T> {
    void addIdentifier(T id);
    void addMultiple(Collection<T> idList);
}
