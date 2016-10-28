package org.lucidj.test;

import org.lucidj.api.Quark;

import java.util.Map;

public class TestService implements Quark
{
    public String toUpperCase (String str)
    {
        return (str.toUpperCase ());
    }

    @Override
    public Map<String, Object> serializeObject ()
    {
        return null;
    }

    @Override
    public void deserializeObject (Map<String, Object> properties)
    {

    }
}

// EOF
