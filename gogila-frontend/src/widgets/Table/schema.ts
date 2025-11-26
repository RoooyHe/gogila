export default {
    type: 'table',
    name: '表格',
    defaultProps: {
        title: '表格标题',
        columns: [
            { label: '姓名', key: 'name' },
            { label: '年龄', key: 'age' },
            { label: '城市', key: 'city' }
        ],
        data: [
            { name: '张三', age: 28, city: '北京' },
            { name: '李四', age: 32, city: '上海' },
            { name: '王五', age: 25, city: '深圳' }
        ],
        pageSize: 10
    },
    defaultSize: { w: 600, h: 300 },
    form: [
        { prop: 'title', label: '标题', type: 'input' },
        { prop: 'pageSize', label: '每页行数', type: 'number' }
    ]
};
