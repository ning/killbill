/*
 * Copyright 2010-2011 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.billing.util.customfield.dao;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.mixins.Transmogrifier;
import org.skife.jdbi.v2.sqlobject.stringtemplate.ExternalizedSqlViaStringTemplate3;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import com.ning.billing.util.customfield.CustomField;
import com.ning.billing.util.customfield.StringCustomField;
import com.ning.billing.util.entity.EntityCollectionDao;

@ExternalizedSqlViaStringTemplate3
@RegisterMapper(FieldStoreDao.CustomFieldMapper.class)
public interface FieldStoreDao extends EntityCollectionDao<CustomField>, Transmogrifier {
    @Override
    @SqlBatch
    public void save(@Bind("objectId") final String objectId,
                     @Bind("objectType") final String objectType,
                     @CustomFieldBinder final List<CustomField> entities);


    public class CustomFieldMapper implements ResultSetMapper<CustomField> {
        @Override
        public CustomField map(int index, ResultSet result, StatementContext context) throws SQLException {
            UUID id = UUID.fromString(result.getString("id"));
            String fieldName = result.getString("field_name");
            String fieldValue = result.getString("field_value");
            return new StringCustomField(id, fieldName, fieldValue);
        }
    }

    @BindingAnnotation(CustomFieldBinder.CustomFieldBinderFactory.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER})
    public @interface CustomFieldBinder {
        public static class CustomFieldBinderFactory implements BinderFactory {
            public Binder build(Annotation annotation) {
                return new Binder<CustomFieldBinder, CustomField>() {
                    public void bind(SQLStatement q, CustomFieldBinder bind, CustomField customField) {
                        q.bind("id", customField.getId().toString());
                        q.bind("fieldName", customField.getName());
                        q.bind("fieldValue", customField.getValue());
                    }
                };
            }
        }
    }
}