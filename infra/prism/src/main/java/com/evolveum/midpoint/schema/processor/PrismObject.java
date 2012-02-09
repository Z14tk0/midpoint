/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;

import javax.xml.namespace.QName;

import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.delta.ObjectDelta;
import com.evolveum.midpoint.schema.delta.PropertyDelta;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;

/**
 * Common supertype for all identity objects. Defines basic properties that each
 * object must have to live in our system (identifier, name).
 * 
 * Objects consists of identifier and name (see definition below) and a set of
 * properties represented as XML elements in the object's body. The attributes
 * are represented as first-level XML elements (tags) of the object XML
 * representation and may be also contained in other tags (e.g. extension,
 * attributes). The QName (namespace and local name) of the element holding the
 * property is considered to be a property name.
 * 
 * This class is named MidPointObject instead of Object to avoid confusion with
 * java.lang.Object.
 * 
 * @author Radovan Semancik
 * 
 */
public class PrismObject<T extends Objectable> extends PrismContainer {

	protected String oid;
	protected String version;
		
	public PrismObject(QName name, PrismObjectDefinition definition, PrismContext prismContext, PropertyPath parentPath) {
		super(name, definition, prismContext, parentPath);
	}

	/**
	 * Returns Object ID (OID).
	 * 
	 * May return null if the object does not have an OID.
	 * 
	 * @return Object ID (OID)
	 */
	public String getOid() {
		return oid;
	}

	public void setOid(String oid) {
		this.oid = oid;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public PrismObjectDefinition<T> getDefinition() {
		return (PrismObjectDefinition<T>) super.getDefinition();
	}
	
	@Override
	public PropertyPath getPath() {
		// Path and parent path are the same for objects (both are empty).
		return getParentPath();
	}

	public Class<T> getJaxbClass() {
		return ((PrismObjectDefinition)getDefinition()).getJaxbClass();
	}

	public T getObjectable() {
		// TODO
		throw new UnsupportedOperationException();
	}
	
	public PrismContainer getExtension() {
		return findPropertyContainer(new QName(getName().getNamespaceURI(), PrismConstants.EXTENSION_LOCAL_NAME));
	}
	
	@Override
	public PrismObject<T> clone() {
		PrismObject<T> clone = new PrismObject<T>(getName(), getDefinition(), prismContext, getParentPath());
		copyValues(clone);
		return clone;
	}

	protected void copyValues(PrismObject<T> clone) {
		super.copyValues(clone);
		clone.oid = this.oid;
	}
	
	public ObjectDelta<T> compareTo(PrismObject<T> other) {
		if (other == null) {
			ObjectDelta<T> objectDelta = new ObjectDelta<T>(getJaxbClass(), ChangeType.DELETE);
			objectDelta.setOid(getOid());
			return objectDelta;
		}
		// This must be a modify
		ObjectDelta<T> objectDelta = new ObjectDelta<T>(getJaxbClass(), ChangeType.MODIFY);
		objectDelta.setOid(getOid());

		Collection<PropertyPath> thisPropertyPaths = listPropertyPaths();
		Collection<PropertyPath> otherPropertyPaths = other.listPropertyPaths();
		Collection<PropertyPath> allPropertyPaths = MiscUtil.union(thisPropertyPaths,otherPropertyPaths);
		
		for (PropertyPath path: allPropertyPaths) {
			PrismProperty thisProperty = findProperty(path);
			PrismProperty otherProperty = other.findProperty(path);
			PropertyDelta propertyDelta = null;
			
			if (thisProperty == null) {
				// this must be an add
				propertyDelta = new PropertyDelta(path);
				// TODO: mangle source
				propertyDelta.addValuesToAdd(otherProperty.getValues());
			} else {
				// TODO: mangle source
				propertyDelta = thisProperty.compareRealValuesTo(otherProperty);
			}
			if (propertyDelta != null && !propertyDelta.isEmpty()) {
				objectDelta.addModification(propertyDelta);
			}
		}
		
		return objectDelta;
	}
	
	/**
	 * Note: hashcode and equals compare the objects in the "java way". That means the objects must be
	 * almost preciselly equal to match (e.g. including source demarcation in values and other "annotations").
	 * For a method that compares the "meaningful" parts of the objects see equivalent(). 
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((oid == null) ? 0 : oid.hashCode());
		return result;
	}

	/**
	 * Note: hashcode and equals compare the objects in the "java way". That means the objects must be
	 * almost preciselly equal to match (e.g. including source demarcation in values and other "annotations").
	 * For a method that compares the "meaningful" parts of the objects see equivalent(). 
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		PrismObject other = (PrismObject) obj;
		if (oid == null) {
			if (other.oid != null)
				return false;
		} else if (!oid.equals(other.oid))
			return false;
		return true;
	}

	/**
	 * this method ignores some part of the object during comparison (e.g. source demarkation in values)
	 * These methods compare the "meaningful" parts of the objects. 
	 */
	public boolean equivalent(Object obj) {
		// Alibistic implementation for now. But shoudl work well.
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		PrismObject other = (PrismObject) obj;
		if (oid == null) {
			if (other.oid != null)
				return false;
		} else if (!oid.equals(other.oid))
			return false;
		ObjectDelta<T> delta = compareTo(other);
		return delta.isEmpty();
	}
	
	/**
	 * Return a human readable name of this class suitable for logs.
	 */
	@Override
	protected String getDebugDumpClassName() {
		return "MidPoint object";
	}
	
	@Override
	protected String additionalDumpDescription() {
		return ", "+getOid();
	}

	public Node serializeToDom() throws SchemaException {
		Node doc = DOMUtil.getDocument();
		serializeToDom(doc);
		return doc;
	}
	
}
