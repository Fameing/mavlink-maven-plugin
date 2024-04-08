package ua.com.miltrex.plugin.generator.definitions.model;

import java.util.List;
import java.util.Objects;

public class MavlinkEnumDef {
    private final String name;
    private final String description;
    private final List<MavlinkEntryDef> entries;
    private final MavlinkDeprecationDef deprecation;

    public MavlinkEnumDef(String name, String description, List<MavlinkEntryDef> entries, MavlinkDeprecationDef deprecation) {
        this.name = name;
        this.description = description;
        this.entries = entries;
        this.deprecation = deprecation;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<MavlinkEntryDef> getEntries() {
        return entries;
    }

    public MavlinkDeprecationDef getDeprecation() {
        return deprecation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MavlinkEnumDef that = (MavlinkEnumDef) o;

        if (!Objects.equals(name, that.name)) return false;
        if (!Objects.equals(description, that.description)) return false;
        if (!Objects.equals(entries, that.entries)) return false;
        return Objects.equals(deprecation, that.deprecation);
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (entries != null ? entries.hashCode() : 0);
        result = 31 * result + (deprecation != null ? deprecation.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MavlinkEnumDef{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", entries=" + entries +
                ", deprecation=" + deprecation +
                '}';
    }
}
