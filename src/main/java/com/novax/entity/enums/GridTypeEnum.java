package com.novax.entity.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum GridTypeEnum {

    ARITHMETIC_GRID(0, "arithmetical"),
    PROPORTIONAL_GRID(1, "geometrical");

    private final int type;
    private final String desc;

    public static String getDescByType(int gridType) {
        for (GridTypeEnum type : GridTypeEnum.values()) {
            if (type.getType()==gridType) {
                return type.getDesc();
            }
        }
        return null;
    }
}
