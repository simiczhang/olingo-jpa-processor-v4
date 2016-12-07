package com.sap.olingo.jpa.metadata.core.edm.mapper.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.EmbeddableType;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.provider.CsdlAnnotation;
import org.apache.olingo.commons.api.edm.provider.annotation.CsdlConstantExpression.ConstantExpressionType;
import org.junit.Before;
import org.junit.Test;

import com.sap.olingo.jpa.metadata.api.JPAEdmMetadataPostProcessor;
import com.sap.olingo.jpa.metadata.core.edm.mapper.exception.ODataJPAModelException;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateEntityTypeAccess;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateNavigationPropertyAccess;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediatePropertyAccess;
import com.sap.olingo.jpa.metadata.core.edm.mapper.extention.IntermediateReferenceList;

public class TestIntermediateDescriptionProperty extends TestMappingRoot {
  private TestHelper helper;
  private IntermediateDescriptionProperty cut;
  private JPAEdmMetadataPostProcessor processor;

  @Before
  public void setup() throws ODataJPAModelException {
    helper = new TestHelper(emf.getMetamodel(), PUNIT_NAME);
    processor = mock(JPAEdmMetadataPostProcessor.class);
    IntermediateModelElement.setPostProcessor(new DefaultEdmPostProcessor());
  }

  @Test
  public void checkProptertyCanBeCreated() throws ODataJPAModelException {
    EmbeddableType<?> et = helper.getEmbeddedableType("PostalAddressData");
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(et, "countryName");
    new IntermediateDescriptionProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute, helper.schema);
  }

  @Test
  public void checkGetProptertyNameOneToMany() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEmbeddedableType("PostalAddressData"),
        "countryName");
    cut = new IntermediateDescriptionProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
        helper.schema);
    assertEquals("Wrong name", "CountryName", cut.getEdmItem().getName());
  }

  @Test
  public void checkGetProptertyNameManyToMany() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEmbeddedableType("PostalAddressData"),
        "regionName");
    cut = new IntermediateDescriptionProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
        helper.schema);
    assertEquals("Wrong name", "RegionName", cut.getEdmItem().getName());
  }

  @Test
  public void checkGetProptertyType() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEmbeddedableType("PostalAddressData"),
        "countryName");
    cut = new IntermediateDescriptionProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
        helper.schema);
    assertEquals("Wrong type", EdmPrimitiveTypeKind.String.getFullQualifiedName().getFullQualifiedNameAsString(),
        cut.getEdmItem().getType());
  }

  @Test
  public void checkGetProptertyIgnoreFalse() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEmbeddedableType("PostalAddressData"),
        "countryName");
    IntermediatePropertyAccess property = new IntermediateDescriptionProperty(new JPAEdmNameBuilder(PUNIT_NAME),
        jpaAttribute,
        helper.schema);
    assertFalse(property.ignore());
  }

  @Test
  public void checkGetProptertyFacetsNullableTrue() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEmbeddedableType("PostalAddressData"),
        "countryName");
    cut = new IntermediateDescriptionProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
        helper.schema);
    assertTrue(cut.getEdmItem().isNullable());
  }

  @Test
  public void checkGetProptertyMaxLength() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEmbeddedableType("PostalAddressData"),
        "countryName");
    cut = new IntermediateDescriptionProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
        helper.schema);
    assertEquals(new Integer(100), cut.getEdmItem().getMaxLength());
  }

  @Test
  public void checkAnnotations() throws ODataJPAModelException {
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEntityType("BusinessPartner"),
        "locationName");
    cut = new IntermediateDescriptionProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
        helper.schema);
    List<CsdlAnnotation> annotations = cut.getEdmItem().getAnnotations();
    assertEquals(1, annotations.size());
    assertEquals("Core.IsLanguageDependent", annotations.get(0).getTerm());
    assertEquals(ConstantExpressionType.Bool, annotations.get(0).getExpression().asConstant().getType());
    assertEquals("true", annotations.get(0).getExpression().asConstant().getValue());
  }

  @Test
  public void checkPostProcessorCalled() throws ODataJPAModelException {
    // PostProcessorSpy spy = new PostProcessorSpy();
    IntermediateModelElement.setPostProcessor(processor);
    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEmbeddedableType("PostalAddressData"),
        "countryName");
    cut = new IntermediateDescriptionProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
        helper.schema);

    cut.getEdmItem();
    verify(processor, atLeastOnce()).processProperty(cut, ADDR_CANONICAL_NAME);
  }

  @Test
  public void checkPostProcessorNameChanged() throws ODataJPAModelException {
    PostProcessorSetName pPDouble = new PostProcessorSetName();
    IntermediateModelElement.setPostProcessor(pPDouble);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEmbeddedableType("PostalAddressData"),
        "countryName");
    cut = new IntermediateDescriptionProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
        helper.schema);

    assertEquals("Wrong name", "CountryDescription", cut.getEdmItem().getName());
  }

  @Test
  public void checkPostProcessorExternalNameChanged() throws ODataJPAModelException {
    PostProcessorSetName pPDouble = new PostProcessorSetName();
    IntermediateModelElement.setPostProcessor(pPDouble);

    Attribute<?, ?> jpaAttribute = helper.getDeclaredAttribute(helper.getEmbeddedableType("PostalAddressData"),
        "countryName");
    IntermediatePropertyAccess property = new IntermediateProperty(new JPAEdmNameBuilder(PUNIT_NAME), jpaAttribute,
        helper.schema);

    assertEquals("Wrong name", "CountryDescription", property.getExternalName());
  }

  private class PostProcessorSetName extends JPAEdmMetadataPostProcessor {

    @Override
    public void processProperty(IntermediatePropertyAccess property, String jpaManagedTypeClassName) {
      if (jpaManagedTypeClassName.equals(ADDR_CANONICAL_NAME)) {
        if (property.getInternalName().equals("countryName")) {
          property.setExternalName("CountryDescription");
        }
      }
    }

    @Override
    public void processNavigationProperty(IntermediateNavigationPropertyAccess property,
        String jpaManagedTypeClassName) {}

    @Override
    public void processEntityType(IntermediateEntityTypeAccess entity) {}

    @Override
    public void provideReferences(IntermediateReferenceList references) throws ODataJPAModelException {}
  }
}
