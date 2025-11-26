export default {
    type: 'stat-card',
    name: '指标卡',
    defaultProps: {
        title: '指标名称',
        unit: '',
        color: '#38bdf8'
    },
    defaultSize: {w: 260, h: 120},
    form: [
        {prop: 'title', label: '标题', type: 'input'},
        {prop: 'unit', label: '单位', type: 'input'},
        {prop: 'color', label: '颜色', type: 'color'}
    ]
};