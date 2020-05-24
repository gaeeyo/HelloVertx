<html>
<head>
    <script src="https://cdn.jsdelivr.net/npm/vue/dist/vue.js"></script>
    <style type="text/css">
        #app table {
            border-collapse: collapse;
        }

        #app table td {
            border: 1px solid #888;
        }

        #app table thead td {
            font-weight: bold;
            background-color: #eee;
        }
    </style>
</head>
<body>
<div id="app">
    <h1>{{ title }}</h1>
    <table>
        <thead>
        <tr>
            <td @click="sort(1)">ファイル名</td>
            <td @click="sort(2)">サイズ</td>
        </tr>
        </thead>
        <tbody>
        <tr v-for="f in files">
            <td v-if="f.isDirectory"><a :href="f.name + '/'">{{ f.name }}/</a></td>
            <td v-else><a :href="f.name">{{ f.name }}</a></td>
            <td v-if="f.isDirectory"></td>
            <td v-else>{{ f.size }}</td>
        </tr>
        </tbody>
    </table>
</div>
<!-- insert -->
<script>
var app = new Vue({
    el: '#app',
    data: {
        sortColumn: 0,
        title: data.title,
        files: data.files
    },
    methods: {
        sort: function (column) {
            if (Math.abs(this.sortColumn) !== column) this.sortColumn = column;
            else this.sortColumn *= -1;

            const reverse = (this.sortColumn > 0 ? 1 : -1)
            switch (Math.abs(column)) {
                case 1:
                    this.files.sort((a, b) => (a.name === b.name ? 0 : (a.name < b.name ? -1 : 1)) * reverse);
                    break;
                case 2:
                    this.files.sort((a, b) => (a.size - b.size) * reverse);
                    break;
            }
        }
    }
});
</script>
</body>
</html>