export default {
    type: 'number-flop',
    name: '数字翻牌',
    defaultProps: {
        value: 9999,
        color: '#38bdf8',
        fontSize: 48
    },
    defaultSize: { w: 200, h: 100 },
    form: [
        { prop: 'value', label: '数值', type: 'number' },
        { prop: 'color', label: '颜色', type: 'color' },
        { prop: 'fontSize', label: '字体大小', type: 'number' }
    ]
};
