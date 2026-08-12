package com.fjxm.print.ui;

final class CategoryLabelFormatter {
    private CategoryLabelFormatter() { }

    static String format(String name) {
        if (name != null && name.startsWith("后厨") && name.endsWith("岗位") && name.length() > 4) {
            return name.substring(2, name.length() - 2);
        }
        return name;
    }
}
