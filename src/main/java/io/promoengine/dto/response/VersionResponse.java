package io.promoengine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionResponse {
    private String version;
    private String droolsVersion;
    private String builtAt;
    private String artifact;
}
