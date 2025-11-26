export default {
    type: 'line-chart',
    name: '折线图',
    defaultProps: {
        title: '折线图标题',
        labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'],
        data: [100, 200, 150, 280, 300],
        color: '#38bdf8'
    },
    defaultSize: { w: 500, h: 300 },
    form: [
        { prop: 'title', label: '标题', type: 'input' },
        { prop: 'color', label: '线条颜色', type: 'color' }
    ]
};