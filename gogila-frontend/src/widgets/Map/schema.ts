export default {
    type: 'map',
    name: '地图',
    defaultProps: {
        title: '地图标题',
        center: [104.065, 37.6952],
        zoom: 4,
        locations: [
            { name: '北京', coord: [116.4074, 39.9042], value: 100 },
            { name: '上海', coord: [121.4737, 31.2304], value: 80 },
            { name: '广州', coord: [113.2644, 23.1291], value: 60 },
            { name: '深圳', coord: [114.0579, 22.5431], value: 70 }
        ]
    },
    defaultSize: { w: 800, h: 500 },
    form: [
        { prop: 'title', label: '标题', type: 'input' },
        { prop: 'zoom', label: '缩放级别', type: 'number' }
    ]
};
