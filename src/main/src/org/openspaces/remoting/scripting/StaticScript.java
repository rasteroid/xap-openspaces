package org.openspaces.remoting.scripting;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;

/**
 * A script that holds the actual script as a String. The name, type, and script must be provided.
 *
 * @author kimchy
 */
public class StaticScript implements Script, Externalizable {

    private String name;

    private String type;

    private String script;

    private Map<String, Object> parameters;

    /**
     * Constructs a new static script. Note, the name, type, and script must be provided.
     */
    public StaticScript() {

    }

    /**
     * Constructs a new static script.
     *
     * @param name   The name of the script.
     * @param type   The type of the script (for example, <code>groovy</code>).
     * @param script The actual script as a String.
     */
    public StaticScript(String name, String type, String script) {
        this.name = name;
        this.type = type;
        this.script = script;
    }

    /**
     * Returns the script as a String.
     */
    public String getScriptAsString() {
        return this.script;
    }

    /**
     * Returns the name of the script. Must uniquely identify a script.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the type of a script. For example: <code>groovy</code>.
     */
    public String getType() {
        return this.type;
    }

    /**
     * Returns the set of parameters that will be passes to the script execution.
     */
    public Map<String, Object> getParameters() {
        return this.parameters;
    }

    /**
     * Sets the name of the script.
     */
    public StaticScript name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the actual script source.
     */
    public StaticScript script(String script) {
        this.script = script;
        return this;
    }

    /**
     * Sets the type of the script. For example: <code>groovy</code>.
     */
    public StaticScript type(String type) {
        this.type = type;
        return this;
    }

    /**
     * Puts a parameter that can be used during script execution.
     *
     * @param name  The name of the parameter.
     * @param value The value of the parameter.
     */
    public StaticScript parameter(String name, Object value) {
        if (parameters == null) {
            parameters = new HashMap<String, Object>();
        }
        parameters.put(name, value);
        return this;
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(name);
        out.writeUTF(type);
        out.writeObject(script);
        if (parameters == null) {
            out.writeBoolean(false);
        } else {
            out.writeBoolean(true);
            out.writeShort(parameters.size());
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                out.writeUTF(entry.getKey());
                out.writeObject(entry.getValue());
            }
        }
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        name = in.readUTF();
        type = in.readUTF();
        script = (String) in.readObject();
        if (in.readBoolean()) {
            int size = in.readShort();
            parameters = new HashMap<String, Object>(size);
            for (int i = 0; i < size; i++) {
                String key = in.readUTF();
                Object value = in.readObject();
                parameters.put(key, value);
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("StaticScript name[").append(getName()).append("]");
        sb.append(" type [").append(getType()).append("]");
        sb.append(" script [").append(script).append("]");
        sb.append(" parameters [").append(parameters).append("]");
        return sb.toString();
    }
}
