package com.moandjiezana.toml;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>Provides access to the keys and tables in a TOML data source.</p>
 *
 * <p>All getters can fall back to default values if they have been provided as a constructor argument.
 * Getters for simple values (String, Date, etc.) will return null if no matching key exists. {@link #getList(String)}, {@link #getTable(String)} and {@link
 * #getTables(String)} return empty values if there is no matching key.</p>
 *
 * <p>All read methods throw an {@link IllegalStateException} if the TOML is incorrect.</p>
 *
 * <p>Example usage:</p>
 * <pre><code>
 * Toml toml = new Toml().read(getTomlFile());
 * String name = toml.getString("name");
 * Long port = toml.getLong("server.ip"); // compound key. Is equivalent to:
 * Long port2 = toml.getTable("server").getLong("ip");
 * MyConfig config = toml.to(MyConfig.class);
 * </code></pre>
 */
public class Toml {

    private static Gson DEFAULT_GSON = new Gson();

    public static void init(Gson gson) {
        DEFAULT_GSON = gson;
    }

    private Map<String, Object> values;
    private final Toml defaults;

    /**
     * Creates Toml instance with no defaults.
     */
    public Toml() {
        this(null);
    }

    /**
     * @param defaults fallback values used when the requested key or table is not present in the TOML source that has been read.
     */
    public Toml(Toml defaults) {
        this(defaults, new HashMap<>());
    }

    /**
     * Populates the current Toml instance with values from file.
     *
     * @param file The File to be read. Expected to be encoded as UTF-8.
     * @return this instance
     * @throws IllegalStateException If file contains invalid TOML
     */
    public Toml read(File file) {
        try (final FileInputStream fileInputStream = new FileInputStream(file)) {
            try (final InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8)) {
                return this.read(inputStreamReader);
            }
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Populates the current Toml instance with values from inputStream.
     *
     * @param inputStream Closed after it has been read.
     * @return this instance
     * @throws IllegalStateException If file contains invalid TOML
     */
    public Toml read(InputStream inputStream) {
        try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream)) {
            return this.read(inputStreamReader);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Populates the current Toml instance with values from reader.
     *
     * @param reader Closed after it has been read.
     * @return this instance
     * @throws IllegalStateException If file contains invalid TOML
     */
    public Toml read(Reader reader) {
        try (final BufferedReader bufferedReader = new BufferedReader(reader)) {
            final StringBuilder w = new StringBuilder();
            String line = bufferedReader.readLine();
            while (line != null) {
                w.append(line).append('\n');
                line = bufferedReader.readLine();
            }
            this.read(w.toString());
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        return this;
    }

    /**
     * Populates the current Toml instance with values from otherToml.
     *
     * @return this instance
     */
    public Toml read(Toml otherToml) {
        this.values = otherToml.values;

        return this;
    }

    /**
     * Populates the current Toml instance with values from tomlString.
     *
     * @param tomlString String to be read.
     * @return this instance
     * @throws IllegalStateException If tomlString is not valid TOML
     */
    public Toml read(String tomlString) throws IllegalStateException {
        final Results results = TomlParser.run(tomlString);
        if (results.errors.hasErrors()) {
            throw new IllegalStateException(results.errors.toString());
        }

        this.values = results.consume();

        return this;
    }

    public String getString(String key) {
        return (String) this.get(key);
    }

    public String getString(String key, String defaultValue) {
        final String val = this.getString(key);
        return val == null ? defaultValue : val;
    }

    public Long getLong(String key) {
        return (Long) this.get(key);
    }

    public Long getLong(String key, Long defaultValue) {
        final Long val = this.getLong(key);
        return val == null ? defaultValue : val;
    }

    /**
     * @param key a TOML key
     * @param <T> type of list items
     * @return <code>null</code> if the key is not found
     */
    public <T> List<T> getList(String key) {
        @SuppressWarnings("unchecked")
        final List<T> list = (List<T>) this.get(key);

        return list;
    }

    /**
     * @param key          a TOML key
     * @param defaultValue a list of default values
     * @param <T>          type of list items
     * @return <code>null</code> is the key is not found
     */
    public <T> List<T> getList(String key, List<T> defaultValue) {
        final List<T> list = this.getList(key);

        return list != null ? list : defaultValue;
    }

    public Boolean getBoolean(String key) {
        return (Boolean) this.get(key);
    }

    public Boolean getBoolean(String key, Boolean defaultValue) {
        final Boolean val = this.getBoolean(key);
        return val == null ? defaultValue : val;
    }

    public Date getDate(String key) {
        return (Date) this.get(key);
    }

    public Date getDate(String key, Date defaultValue) {
        final Date val = this.getDate(key);
        return val == null ? defaultValue : val;
    }

    public Double getDouble(String key) {
        return (Double) this.get(key);
    }

    public Double getDouble(String key, Double defaultValue) {
        final Double val = this.getDouble(key);
        return val == null ? defaultValue : val;
    }

    /**
     * @param key A table name, not including square brackets.
     * @return A new Toml instance or <code>null</code> if no value is found for key.
     */
    @SuppressWarnings("unchecked")
    public Toml getTable(String key) {
        final Map<String, Object> map = (Map<String, Object>) this.get(key);

        return map != null ? new Toml(null, map) : null;
    }

    /**
     * @param key Name of array of tables, not including square brackets.
     * @return A {@link List} of Toml instances or <code>null</code> if no value is found for key.
     */
    @SuppressWarnings("unchecked")
    public List<Toml> getTables(String key) {
        final List<Map<String, Object>> tableArray = (List<Map<String, Object>>) this.get(key);

        if (tableArray == null) {
            return null;
        }

        final ArrayList<Toml> tables = new ArrayList<>();

        for (Map<String, Object> table : tableArray) {
            tables.add(new Toml(null, table));
        }

        return tables;
    }

    /**
     * @param key a key name, can be compound (eg. a.b.c)
     * @return true if key is present
     */
    public boolean contains(String key) {
        return this.get(key) != null;
    }

    /**
     * @param key a key name, can be compound (eg. a.b.c)
     * @return true if key is present and is a primitive
     */
    public boolean containsPrimitive(String key) {
        final Object object = this.get(key);

        return object != null && !(object instanceof Map) && !(object instanceof List);
    }

    /**
     * @param key a key name, can be compound (eg. a.b.c)
     * @return true if key is present and is a table
     */
    public boolean containsTable(String key) {
        final Object object = this.get(key);

        return (object instanceof Map);
    }

    /**
     * @param key a key name, can be compound (eg. a.b.c)
     * @return true if key is present and is a table array
     */
    public boolean containsTableArray(String key) {
        final Object object = this.get(key);

        return (object instanceof List);
    }

    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    /**
     * <p>
     * Populates an instance of targetClass with the values of this Toml instance. The target's field names must match keys or tables. Keys not present in
     * targetClass will be ignored.
     * </p>
     *
     * <p>Tables are recursively converted to custom classes or to {@link Map Map&lt;String, Object&gt;}.</p>
     *
     * <p>In addition to straight-forward conversion of TOML primitives, the following are also available:</p>
     *
     * <ul>
     *  <li>Integer -&gt; int, long (or wrapper), {@link java.math.BigInteger}</li>
     *  <li>Float -&gt; float, double (or wrapper), {@link java.math.BigDecimal}</li>
     *  <li>One-letter String -&gt; char, {@link Character}</li>
     *  <li>String -&gt; {@link String}, enum, {@link java.net.URI}, {@link java.net.URL}</li>
     *  <li>Multiline and Literal Strings -&gt; {@link String}</li>
     *  <li>Array -&gt; {@link List}, {@link Set}, array. The generic type can be anything that can be converted.</li>
     *  <li>Table -&gt; Custom class, {@link Map Map&lt;String, Object&gt;}</li>
     * </ul>
     *
     * @param targetClass Class to deserialize TOML to.
     * @param <T>         type of targetClass.
     * @return A new instance of targetClass.
     */
    public <T> T to(Class<T> targetClass) {
        final JsonElement json = DEFAULT_GSON.toJsonTree(this.toMap());

        if (targetClass == JsonElement.class) {
            return targetClass.cast(json);
        }

        return DEFAULT_GSON.fromJson(json, targetClass);
    }

    public Map<String, Object> toMap() {
        final HashMap<String, Object> valuesCopy = new HashMap<>(this.values);

        if (this.defaults != null) {
            for (Map.Entry<String, Object> entry : this.defaults.values.entrySet()) {
                if (!valuesCopy.containsKey(entry.getKey())) {
                    valuesCopy.put(entry.getKey(), entry.getValue());
                }
            }
        }

        return valuesCopy;
    }

    /**
     * @return a {@link Set} of Map.Entry instances. Modifications to the {@link Set} are not reflected in this Toml instance. Entries are immutable, so {@link
     * Map.Entry#setValue(Object)} throws an UnsupportedOperationException.
     */
    public Set<Map.Entry<String, Object>> entrySet() {
        final Set<Map.Entry<String, Object>> entries = new LinkedHashSet<>();

        for (Map.Entry<String, Object> entry : this.values.entrySet()) {
            final Class<?> entryClass = entry.getValue().getClass();

            if (Map.class.isAssignableFrom(entryClass)) {
                entries.add(new Entry(entry.getKey(), this.getTable(entry.getKey())));
            } else if (List.class.isAssignableFrom(entryClass)) {
                final List<?> value = (List<?>) entry.getValue();
                if (!value.isEmpty() && value.get(0) instanceof Map) {
                    entries.add(new Entry(entry.getKey(), this.getTables(entry.getKey())));
                } else {
                    entries.add(new Entry(entry.getKey(), value));
                }
            } else {
                entries.add(new Entry(entry.getKey(), entry.getValue()));
            }
        }

        return entries;
    }

    private record Entry(String key, Object value) implements Map.Entry<String, Object> {

        @Override
        public String getKey() {
            return this.key;
        }

        @Override
        public Object getValue() {
            return this.value;
        }

        @Override
        public Object setValue(Object value) {
            throw new UnsupportedOperationException("TOML entry values cannot be changed.");
        }

    }

    @SuppressWarnings("unchecked")
    private Object get(String key) {
        if (this.values.containsKey(key)) {
            return this.values.get(key);
        }

        Object current = new HashMap<>(this.values);

        final Keys.Key[] keys = Keys.split(key);

        for (Keys.Key k : keys) {
            if (k.index == -1 && current instanceof Map && ((Map<String, Object>) current).containsKey(k.path)) {
                return ((Map<String, Object>) current).get(k.path);
            }

            //noinspection ConstantConditions
            current = ((Map<String, Object>) current).get(k.name);

            if (k.index > -1 && current != null) {
                if (k.index >= ((List<?>) current).size()) {
                    return null;
                }

                current = ((List<?>) current).get(k.index);
            }

            if (current == null) {
                return this.defaults != null ? this.defaults.get(key) : null;
            }
        }

        return current;
    }

    private Toml(Toml defaults, Map<String, Object> values) {
        this.values = values;
        this.defaults = defaults;
    }
}
