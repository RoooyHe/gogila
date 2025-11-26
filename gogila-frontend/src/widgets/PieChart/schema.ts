export default {
    type: 'pie-chart',
    name: '饼图',
    defaultProps: {
        title: '饼图标题',
        data: [
            { name: '分类A', value: 100 },
            { name: '分类B', value: 200 },
            { name: '分类C', value: 150 }
        ],
        colors: ['#38bdf8', '#8b5cf6', '#ec4899']
    },
    defaultSize: { w: 400, h: 300 },
    form: [
        { prop: 'title', label: '标题', type: 'input' }
    ]
};
