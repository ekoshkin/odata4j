package org.odata4j.consumer;

import java.util.ArrayList;
import java.util.List;

import org.core4j.Enumerable;
import org.odata4j.core.OEntityRef;
import org.odata4j.edm.EdmDataServices;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmNavigationProperty;
import org.odata4j.format.xml.AtomFeedFormatParser.AtomEntry;
import org.odata4j.format.xml.AtomFeedFormatParser.DataServicesAtomEntry;
import org.odata4j.internal.EntitySegment;
import org.odata4j.internal.FeedCustomizationMapping;
import org.odata4j.internal.InternalUtil;

public class OEntityRefImpl<T> implements OEntityRef<T> {

    private final boolean isDelete;
    private final ODataClient client;
    private final Class<T> entityType;
    private final EdmDataServices metadata;
    private final String serviceRootUri;
    private final List<EntitySegment> segments = new ArrayList<EntitySegment>();

    private final FeedCustomizationMapping fcMapping;
   
    public OEntityRefImpl(boolean isDelete, ODataClient client, Class<T> entityType, String serviceRootUri, EdmDataServices metadata, String entitySetName, Object[] key, FeedCustomizationMapping fcMapping) {
        this.isDelete = isDelete;
        this.client = client;
        this.entityType = entityType;
        this.serviceRootUri = serviceRootUri;
        this.metadata = metadata;
        
        segments.add(new EntitySegment(entitySetName, key));
        
        this.fcMapping = fcMapping;
    }

    @Override
    public OEntityRef<T> nav(String navProperty, Object... key) {
        segments.add(new EntitySegment(navProperty, key));
        return this;
    }
    
    @Override
    public OEntityRef<T> nav(String navProperty) {
        segments.add(new EntitySegment(navProperty, null));
        return this;
    }

    @Override
    public T execute() {

        String path = Enumerable.create(segments).join("/");

        if (isDelete) {
            ODataClientRequest request = ODataClientRequest.delete(serviceRootUri + path);
            client.deleteEntity(request);
            return null;

        } else {

            ODataClientRequest request = ODataClientRequest.get(serviceRootUri + path);

            AtomEntry entry = client.getEntity(request);
            if (entry == null)
                return null;
            DataServicesAtomEntry dsae = (DataServicesAtomEntry) entry;
            
        	//	the first segment contains the entitySetName we start from
        	EdmEntitySet entitySet = metadata.getEdmEntitySet(segments.get(0).segment);
        	for(EntitySegment segment : segments.subList(1, segments.size()) ) {
        		EdmNavigationProperty navProperty = entitySet.type
        			.getNavigationProperty(segment.segment);
        		entitySet = metadata.getEdmEntitySet(navProperty.toRole.type);
        	}

            return (T) InternalUtil.toEntity(entityType, metadata, entitySet, dsae,fcMapping);
        }
    }

}