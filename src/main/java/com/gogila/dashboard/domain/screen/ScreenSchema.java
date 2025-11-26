package com.gogila.dashboard.domain.screen;

import java.util.List;
import java.util.Map;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @Author Roy @DataTime 2025/11/26 11:49 @Mail roooyhe@163.com
 */
@Document("screen_schema")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreenSchema {

  @Id private String id;

  /** 用于前端路由访问的大屏编码，例如：index-overview */
  private String code;

  /** 大屏名称 */
  private String name;

  /** 画布配置，如宽高、背景色、背景图等 */
  private Map<String, Object> canvasConfig;

  /** 组件列表 每个组件一个简化 schema（你后续再细化） */
  private List<WidgetConfig> widgets;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class WidgetConfig {
    private String id;
    private String type; // 如 "stat-card" / "line-chart"
    private Map<String, Object> props; // 组件属性，如标题、颜色等
    private Map<String, Object> position; // x,y,w,h
    private DataBinding dataBinding; // 数据绑定（后端数据集标识）
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DataBinding {
    private String mode; // "http" / "websocket"
    private String api; // 如 "/api/data/online-users"
    private String topic; // 如果是 websocket，可以是 topic 名
    private Integer interval; // 轮询间隔（毫秒，可选）
  }
}