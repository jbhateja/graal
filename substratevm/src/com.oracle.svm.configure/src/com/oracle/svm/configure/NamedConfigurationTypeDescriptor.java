/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.svm.configure;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import jdk.graal.compiler.util.json.JsonWriter;

public record NamedConfigurationTypeDescriptor(String name) implements ConfigurationTypeDescriptor {

    public static NamedConfigurationTypeDescriptor fromJSONName(String jsonName) {
        if (!ClassNameSupport.isValidTypeName(jsonName) && ClassNameSupport.isValidReflectionName(jsonName)) {
            return fromReflectionName(jsonName);
        }
        return fromTypeName(jsonName);
    }

    public static NamedConfigurationTypeDescriptor fromTypeName(String typeName) {
        Objects.requireNonNull(typeName);
        return new NamedConfigurationTypeDescriptor(typeName);
    }

    public static NamedConfigurationTypeDescriptor fromReflectionName(String reflectionName) {
        return fromTypeName(ClassNameSupport.reflectionNameToTypeName(reflectionName));
    }

    public static NamedConfigurationTypeDescriptor fromJNIName(String jniName) {
        return fromTypeName(ClassNameSupport.jniNameToTypeName(jniName));
    }

    @Override
    public Kind getDescriptorType() {
        return Kind.NAMED;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public Collection<String> getAllQualifiedJavaNames() {
        return Collections.singleton(name);
    }

    @Override
    public int compareTo(ConfigurationTypeDescriptor other) {
        if (other instanceof NamedConfigurationTypeDescriptor namedOther) {
            return name.compareTo(namedOther.name);
        } else {
            return getDescriptorType().compareTo(other.getDescriptorType());
        }
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof NamedConfigurationTypeDescriptor that)) {
            return false;
        }
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    @Override
    public void printJson(JsonWriter writer) throws IOException {
        writer.quote(name);
    }
}
