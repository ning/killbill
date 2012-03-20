package com.ning.billing.util.entity;

import com.ning.billing.util.CallContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;

import java.util.List;

public interface UpdatableEntityCollectionDao<T extends Entity> extends EntityCollectionDao<T> {
    @SqlBatch(transactional=false)
    public void batchUpdateFromTransaction(@Bind("objectId") final String objectId,
                                           @Bind("objectType") final String objectType,
                                           @BindBean final List<T> entities,
                                           @CallContextBinder final CallContext context);
}