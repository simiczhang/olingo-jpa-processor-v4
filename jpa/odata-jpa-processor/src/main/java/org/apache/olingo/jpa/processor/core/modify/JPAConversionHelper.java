package org.apache.olingo.jpa.processor.core.modify;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAAttribute;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAEntityType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAPath;
import org.apache.olingo.jpa.metadata.core.edm.mapper.api.JPAStructuredType;
import org.apache.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessException;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessorException;
import org.apache.olingo.jpa.processor.core.exception.ODataJPAProcessorException.MessageKeys;
import org.apache.olingo.jpa.processor.core.query.ExpressionUtil;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.serializer.SerializerException;

/**
 * Helper method for modifying requests.<p>
 * Mainly created to increase testability
 * @author Oliver Grande
 *
 */
public class JPAConversionHelper {

  private final Map<Object, Map<String, Object>> getterBuffer;

  public JPAConversionHelper() {
    super();
    this.getterBuffer = new HashMap<Object, Map<String, Object>>();
  }

  /**
   * Creates a map of attribute name and the return value of there getter method. <p>
   * It is assumed that the method name is composed from <i>get</> and the
   * name of the attribute and that the attribute name starts with a lower case
   * letter.
   * @param instance
   * @return
   * @throws ODataJPAProcessorException
   */
  public Map<String, Object> buildGetterMap(Object instance) throws ODataJPAProcessorException {

    if (instance != null) {

      Map<String, Object> getterMap = getterBuffer.get(instance);
      if (getterMap == null) {
        getterMap = new HashMap<String, Object>();
        Method[] methods = instance.getClass().getMethods();
        for (Method meth : methods) {
          if (meth.getName().substring(0, 3).equals("get")) {
            String attributeName = meth.getName().substring(3, 4).toLowerCase() + meth.getName().substring(4);
            try {
              Object value = meth.invoke(instance);
              getterMap.put(attributeName, value);
            } catch (IllegalAccessException e) {
              throw new ODataJPAProcessorException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
            } catch (IllegalArgumentException e) {
              throw new ODataJPAProcessorException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
            } catch (InvocationTargetException e) {
              throw new ODataJPAProcessorException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
            }
          }
        }
        getterBuffer.put(instance, getterMap);
      }
      return getterMap;
    } else
      throw new ODataJPAProcessorException(MessageKeys.PARAMETER_NULL, HttpStatusCode.INTERNAL_SERVER_ERROR);
  }

  /**
   * Converts the payload of a request into the corresponding odata entity
   * @param odata
   * @param request
   * @param requestFormat
   * @param edmEntitySet
   * @return
   * @throws DeserializerException
   */
  public Entity convertInputStream(final OData odata, final ODataRequest request, final ContentType requestFormat,
      EdmEntitySet edmEntitySet) throws ODataJPAProcessorException {

    InputStream requestInputStream = request.getBody();
    DeserializerResult result;
    try {
      ODataDeserializer deserializer = odata.createDeserializer(requestFormat);
      result = deserializer.entity(requestInputStream, edmEntitySet.getEntityType());
    } catch (DeserializerException e) {
      throw new ODataJPAProcessorException(e, HttpStatusCode.BAD_REQUEST);
    }
    Entity requestEntity = result.getEntity();
    return requestEntity;
  }

  /**
   * Creates nested map of attributes and there (new) values. Primitive values are instances of e.g. Integer. Embedded
   * Types are returned as maps.
   * 
   * @param odata
   * @param st
   * @param odataProperties
   * @return
   * @throws ODataJPAProcessException
   */
  public Map<String, Object> convertProperties(final OData odata, final JPAStructuredType st,
      List<Property> odataProperties) throws ODataJPAProcessException {

    final Map<String, Object> jpaAttributes = new HashMap<String, Object>();
    String internalName = null;
    Object jpaAttribute = null;
    for (Property odataProperty : odataProperties) {
      switch (odataProperty.getValueType()) {
      case COMPLEX:
        try {
          JPAPath path = st.getPath(odataProperty.getName());
          internalName = path.getPath().get(0).getInternalName();
          JPAStructuredType a = st.getAttribute(internalName).getStructuredType();
          jpaAttribute = convertProperties(odata, a, ((ComplexValue) odataProperty.getValue()).getValue());
        } catch (ODataJPAModelException e) {
          throw new ODataJPAProcessorException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
        }
        break;
      case PRIMITIVE:
        try {
          final JPAAttribute attribute = st.getPath(odataProperty.getName()).getLeaf();
          internalName = attribute.getInternalName();
          jpaAttribute = ExpressionUtil.convertValueOnAttribute(odata, attribute, odataProperty.getValue().toString(),
              false);
        } catch (ODataJPAModelException e) {
          throw new ODataJPAProcessorException(e, HttpStatusCode.INTERNAL_SERVER_ERROR);
        }
        break;
      default:
        throw new ODataJPAProcessorException(MessageKeys.NOT_SUPPORTED_PROP_TYPE, HttpStatusCode.NOT_IMPLEMENTED,
            odataProperty.getValueType().name());
      }
      jpaAttributes.put(internalName, jpaAttribute);
    }
    return jpaAttributes;
  }

  public String convertKeyToLocal(final OData odata, final ODataRequest request, EdmEntitySet edmEntitySet,
      JPAEntityType et, Object primaryKey) throws SerializerException, ODataJPAProcessorException {

    Entity createdEntity = new Entity();

    try {
      final List<JPAPath> keyPath = et.getKeyPath();
      final List<Property> properties = createdEntity.getProperties();

      collectKeyProperties(primaryKey, keyPath, properties);
    } catch (ODataJPAModelException e) {
      throw new ODataJPAProcessorException(e, HttpStatusCode.BAD_REQUEST);
    }

    final String location = request.getRawBaseUri() + '/'
        + odata.createUriHelper().buildCanonicalURL(edmEntitySet, createdEntity);
    return location;
  }

  private void collectKeyProperties(Object primaryKey, final List<JPAPath> keyPath, final List<Property> properties)
      throws ODataJPAProcessorException, ODataJPAModelException {
    if (keyPath.size() > 1) {
      final Map<String, Object> getter = buildGetterMap(primaryKey);

      for (JPAPath key : keyPath) {
        final Property property = new Property(null, key.getLeaf().getExternalName());
        property.setValue(ValueType.PRIMITIVE, getter.get(key.getLeaf().getInternalName()));
        properties.add(property);
      }
    } else {
      JPAPath key = keyPath.get(0);
      if (key.getLeaf().isComplex()) {
        // EmbeddedId
        collectKeyProperties(primaryKey, key.getLeaf().getStructuredType().getPathList(), properties);
      } else {
        final Property property = new Property(null, key.getLeaf().getExternalName());
        property.setValue(ValueType.PRIMITIVE, primaryKey);
        properties.add(property);
      }
    }
  }
}