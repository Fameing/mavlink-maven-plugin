package ua.com.miltrex.plugin.generator.definitions.model;

import java.util.List;
import java.util.Objects;

public class MavlinkMessageDef {
    private final int id;
    private final String name;
    private final String description;
    private final List<MavlinkFieldDef> fields;
    private final MavlinkDeprecationDef deprecation;
    private final boolean workInProgress;

    public MavlinkMessageDef(
            int id,
            String name,
            String description,
            List<MavlinkFieldDef> fields,
            MavlinkDeprecationDef deprecation,
            boolean workInProgress) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.fields = fields;
        this.deprecation = deprecation;
        this.workInProgress = workInProgress;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<MavlinkFieldDef> getFields() {
        return fields;
    }

    public MavlinkDeprecationDef getDeprecation() {
        return deprecation;
    }

    public boolean isWorkInProgress() {
        return workInProgress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MavlinkMessageDef that = (MavlinkMessageDef) o;

        if (id != that.id) return false;
        if (workInProgress != that.workInProgress) return false;
        if (!Objects.equals(name, that.name)) return false;
        if (!Objects.equals(description, that.description)) return false;
        if (!Objects.equals(fields, that.fields)) return false;
        return Objects.equals(deprecation, that.deprecation);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (fields != null ? fields.hashCode() : 0);
        result = 31 * result + (deprecation != null ? deprecation.hashCode() : 0);
        result = 31 * result + (workInProgress ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MavlinkMessageDef{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", fields=" + fields +
                ", deprecation=" + deprecation +
                ", workInProgress=" + workInProgress +
                '}';
    }
}
