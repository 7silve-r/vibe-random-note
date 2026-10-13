package com.silver.common;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageResult<T> {
    private Long total; // 符合查询条件的总记录数
    private List<T> list;
}
