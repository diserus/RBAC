public record Permission(String name, String resource, String description) {
    public Permission {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Permission name не должно быть null или empty");
        }
        if (name.contains(" ")) {
            throw new IllegalArgumentException("Permission name не должно содержать пробелы");
        }
        name = name.trim().toUpperCase();

        if (resource == null || resource.trim().isEmpty()) {
            throw new IllegalArgumentException("Permission resource не должен быть null или empty");
        }
        resource = resource.trim().toLowerCase();

        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Permission description не должно быть null или empty");
        }
        description = description.trim();
    }
}
