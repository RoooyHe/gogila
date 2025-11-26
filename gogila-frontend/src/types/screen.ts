export interface DataBinding {
    mode: 'http' | 'websocket';
    api?: string;
    topic?: string;
    interval?: number;
}

export interface WidgetConfig {
    id: string;
    type: string;
    props: Record<string, any>;
    position: {
        x: number;
        y: number;
        w: number;
        h: number;
    };
    dataBinding?: DataBinding;
}

export interface ScreenSchema {
    id?: string;
    code: string;
    name: string;
    canvasConfig?: Record<string, any>;
    widgets: WidgetConfig[];
}