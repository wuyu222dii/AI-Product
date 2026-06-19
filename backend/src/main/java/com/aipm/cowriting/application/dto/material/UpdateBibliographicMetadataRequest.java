package com.aipm.cowriting.application.dto.material;

import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateBibliographicMetadataRequest(
        List<@Size(max = 120, message = "作者名称不能超过 120 个字符") String> authors,
        @Size(max = 40, message = "year 长度不能超过 40")
        String year,
        @Size(max = 500, message = "title 长度不能超过 500")
        String title,
        @Size(max = 500, message = "sourceTitle 长度不能超过 500")
        String sourceTitle,
        @Size(max = 300, message = "publisher 长度不能超过 300")
        String publisher,
        @Size(max = 1000, message = "url 长度不能超过 1000")
        String url,
        @Size(max = 200, message = "doi 长度不能超过 200")
        String doi,
        @Size(max = 80, message = "publicationType 长度不能超过 80")
        String publicationType
) {
}
