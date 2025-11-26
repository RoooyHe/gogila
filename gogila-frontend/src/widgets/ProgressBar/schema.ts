export default {
    type: 'progress-bar',
    name: '进度条',
    defaultProps: {
        title: '进度',
        percentage: 65,
        color: '#38bdf8',
        showText: true
    },
    defaultSize: { w: 300, h: 80 },
    form: [
        { prop: 'title', label: '标题', type: 'input' },
        { prop: 'percentage', label: '百分比 (0-100)', type: 'number' },
        { prop: 'color', label: '颜色', type: 'color' },
        { prop: 'showText', label: '显示百分比', type: 'checkbox' }
    ]
};
