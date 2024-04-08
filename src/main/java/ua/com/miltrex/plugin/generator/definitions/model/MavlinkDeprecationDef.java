package ua.com.miltrex.plugin.generator.definitions.model;

import java.util.Objects;

public class MavlinkDeprecationDef {
    private final String since;
    private final String replacedBy;
    private final String message;

    public MavlinkDeprecationDef(String since, String replacedBy, String message) {
        this.since = since;
        this.replacedBy = replacedBy;
        this.message = message;
    }

    public String getSince() {
        return since;
    }

    public String getReplacedBy() {
        return replacedBy;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MavlinkDeprecationDef that = (MavlinkDeprecationDef) o;

        if (!Objects.equals(since, that.since)) return false;
        if (!Objects.equals(replacedBy, that.replacedBy)) return false;
        return Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        int result = since != null ? since.hashCode() : 0;
        result = 31 * result + (replacedBy != null ? replacedBy.hashCode() : 0);
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MavlinkDeprecationDef{" +
                "since='" + since + '\'' +
                ", replacedBy='" + replacedBy + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
