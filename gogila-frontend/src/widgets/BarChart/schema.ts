export default {
    type: 'bar-chart',
    name: '柱状图',
    defaultProps: {
        title: '柱状图标题',
        labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'],
        data: [120, 200, 150, 280, 300],
        color: '#38bdf8'
    },
    defaultSize: { w: 500, h: 300 },
    form: [
        { prop: 'title', label: '标题', type: 'input' },
        { prop: 'color', label: '柱子颜色', type: 'color' }
    ]
};
