/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2014 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.testing.mock.aem;

import static com.day.cq.wcm.api.NameConstants.PN_IS_CONTAINER;
import static org.apache.sling.api.resource.ResourceResolver.PROPERTY_RESOURCE_TYPE;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.adapter.SlingAdaptable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.jetbrains.annotations.NotNull;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.components.Component;
import com.day.cq.wcm.api.components.ComponentEditConfig;
import com.day.cq.wcm.api.components.VirtualComponent;

/**
 * Mock implementation of {@link Component}.
 */
@SuppressWarnings("null")
class MockComponent extends SlingAdaptable implements Component {

  private final Resource resource;
  private final ValueMap props;

  private Component superComponent;
  private boolean superComponentInitialized;

  MockComponent(@NotNull Resource resource) {
    this.resource = resource;
    this.props = new RemoveKeyPrefixMap(new HashMap<>(ResourceUtil.getValueMap(resource)));
  }

  @Override
  public String getPath() {
    return resource.getPath();
  }

  @Override
  public String getName() {
    return resource.getName();
  }

  @Override
  public String getTitle() {
    return props.get(JcrConstants.JCR_TITLE, String.class);
  }

  @Override
  public String getDescription() {
    return props.get(JcrConstants.JCR_DESCRIPTION, String.class);
  }

  @Override
  public ValueMap getProperties() {
    return props;
  }

  @Override
  public String getResourceType() {
    return Optional.ofNullable(this.props.get(PROPERTY_RESOURCE_TYPE, String.class))
            .orElseGet(() -> Optional.of(this.getPath())
                    .filter(path -> path.startsWith("/"))
                    .flatMap(path -> Arrays.stream(this.resource.getResourceResolver().getSearchPath())
                            .filter(path::startsWith)
                            .map(searchPath -> path.substring(searchPath.length()))
                            .findFirst())
                    .orElseGet(this::getPath));
  }

  @Override
  public boolean isAccessible() {
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
    if (type == Resource.class) {
      return (AdapterType)resource;
    }
    return super.adaptTo(type);
  }

  @Override
  public String getComponentGroup() {
    return props.get(NameConstants.PN_COMPONENT_GROUP, String.class);
  }

  @Override
  public boolean noDecoration() {
    return props.get(NameConstants.PN_NO_DECORATION, false);
  }

  @Override
  public Map<String, String> getHtmlTagAttributes() {
    Map<String,String> attrs = new HashMap<>();
    Resource htmlTagChild = resource.getChild(NameConstants.NN_HTML_TAG);
    if (htmlTagChild != null) {
      ValueMap htmlTagProps = htmlTagChild.getValueMap();
      Set<String> keySet = htmlTagProps.keySet();
      for (String key : keySet) {
        String value = htmlTagProps.get(key, String.class);
        if (value != null) {
          attrs.put(key, value);
        }
      }
    }
    return Collections.unmodifiableMap(attrs);
  }

  @Override
  public Component getSuperComponent() {
    if (!superComponentInitialized) {
      String resourceSuperType = resource.getResourceSuperType();
      if (StringUtils.isNotEmpty(resourceSuperType)) {
        Resource superResource = resource.getResourceResolver().getResource(resourceSuperType);
        if (superResource != null) {
          superComponent = new MockComponent(superResource);
        }
      }
      superComponentInitialized = true;
    }
    return superComponent;
  }

  @Override
  public Resource getLocalResource(String name) {
    return resource.getChild(name);
  }

  @Override
  public boolean isContainer() {
    return this.props.get(PN_IS_CONTAINER, getSuperComponent() != null && getSuperComponent().isContainer());
  }

  // --- unsupported operations ---

  @Override
  public String getCellName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isEditable() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isDesignable() {
    throw new UnsupportedOperationException();
  }


  @Override
  public boolean isAnalyzable() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getDialogPath() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getDesignDialogPath() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getIconPath() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getThumbnailPath() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ComponentEditConfig getDeclaredEditConfig() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ComponentEditConfig getDeclaredChildEditConfig() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ComponentEditConfig getEditConfig() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ComponentEditConfig getChildEditConfig() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ComponentEditConfig getDesignEditConfig(String cellName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<VirtualComponent> getVirtualComponents() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getDefaultView() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getTemplatePath() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String[] getInfoProviders() {
    throw new UnsupportedOperationException();
  }

  /**
   * Emulate behavior of AEM code: Remove './' prefix from all keys in the map.
   */
  private static class RemoveKeyPrefixMap extends ValueMapDecorator {

    RemoveKeyPrefixMap(Map<String, Object> base) {
      super(base);
    }

    @Override
    public <T> T get(@NotNull String name, Class<T> type) {
      return super.get(removeKeyPrefix(name), type);
    }

    @Override
    public <T> T get(@NotNull String name, T defaultValue) {
      return super.get(removeKeyPrefix(name), defaultValue);
    }

    @Override
    public Object get(Object key) {
      return super.get(sanitizeKey(key));
    }

    @Override
    public Object remove(Object key) {
      return super.remove(sanitizeKey(key));
    }

    @Override
    public Object put(String key, Object value) {
      return super.put(removeKeyPrefix(key), value);
    }

    @Override
    public boolean containsKey(Object key) {
      return super.containsKey(sanitizeKey(key));
    }

    private String removeKeyPrefix(String key) {
      if (key == null) {
        return null;
      }
      if (key.startsWith("./")) {
        return key.substring(2);
      }
      return key;
    }

    private Object sanitizeKey(Object key) {
      if (key instanceof String) {
        return removeKeyPrefix((String)key);
      }
      return key;
    }

  }

}
