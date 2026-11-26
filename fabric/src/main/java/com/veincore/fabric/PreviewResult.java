package com.veincore.fabric;

import com.veincore.core.Pos;

import java.util.List;

/** 服务端预览扫描结果（M4）：status ∈ idle / unauthorized / cooldown / no_target / ready / over600。 */
public record PreviewResult(String pattern, String status, int total, int cooldownTicks, List<Pos> blocks) {
}
