/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo5j.
 *
 * Neo5j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo5j.jmx.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.ReflectionException;

import org.neo5j.configuration.ConfigValue;
import org.neo5j.jmx.Description;
import org.neo5j.kernel.internal.KernelData;

@Description( "The configuration parameters used to configure Neo5j" )
public final class ConfigurationBean extends Neo5jMBean
{
    public static final String CONFIGURATION_MBEAN_NAME = "Configuration";
    private final Map<String,ConfigValue> config;

    ConfigurationBean( KernelData kernel, ManagementSupport support ) throws NotCompliantMBeanException
    {
        super( CONFIGURATION_MBEAN_NAME, kernel, support );
        this.config = kernel.getConfig().getConfigValues();
    }

    private String describeConfigParameter( String param )
    {
        return config.get( param ).description().orElse( "Configuration attribute" );
    }
    private MBeanAttributeInfo[] keys()
    {
        List<MBeanAttributeInfo> keys = new ArrayList<>();
        for ( String key : config.keySet() )
        {
            keys.add( new MBeanAttributeInfo( key, String.class.getName(),
                    describeConfigParameter( key ), true, false, false ) );
        }
        return keys.toArray( new MBeanAttributeInfo[keys.size()] );
    }

    @Override
    public Object getAttribute( String attribute ) throws AttributeNotFoundException, MBeanException,
            ReflectionException
    {
        return config.get( attribute ).value().orElse( null );
    }

    @Override
    public AttributeList getAttributes( String[] attributes )
    {
        AttributeList result = new AttributeList( attributes.length );
        for ( String attribute : attributes )
        {
            try
            {
                result.add( new Attribute( attribute, getAttribute( attribute ) ) );
            }
            catch ( Exception e )
            {
                throw new RuntimeException( e );
            }
        }
        return result;
    }

    @Override
    public void setAttribute( Attribute attribute )
            throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException
    {
        throw new InvalidAttributeValueException( "Not a writeable attribute: " + attribute.getName() );
    }

    @Override
    public MBeanInfo getMBeanInfo()
    {
        Description description = getClass().getAnnotation( Description.class );
        return new MBeanInfo( getClass().getName(), description != null ? description.value() : "Neo5j configuration",
                keys(), null, null, null );
    }

    @Override
    public Object invoke( String s, Object[] objects, String[] strings )
            throws MBeanException, ReflectionException
    {
        try
        {
            return getClass().getMethod( s ).invoke( this );
        }
        catch( InvocationTargetException e )
        {
            throw new MBeanException( (Exception) e.getTargetException() );
        }
        catch( Exception e )
        {
            throw new MBeanException( e );
        }
    }
}
