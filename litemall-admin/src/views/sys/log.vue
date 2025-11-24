<template>
  <div class="app-container">

    <!-- 查询和其他操作 -->
    <div class="filter-container">
      <el-input v-model="listQuery.name" clearable class="filter-item" style="width: 200px;" :placeholder="$t('sys_log.placeholder.filter_name')"/>
      <el-select v-model="listQuery.status" clearable class="filter-item" style="width: 130px;" :placeholder="$t('sys_log.placeholder.filter_status')">
        <el-option label="成功" :value="true"/>
        <el-option label="失败" :value="false"/>
      </el-select>
      <el-date-picker
        v-model="timeRange"
        class="filter-item"
        type="datetimerange"
        :picker-options="pickerOptions"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        value-format="yyyy-MM-dd HH:mm:ss"
        style="width: 380px;"
        @change="handleTimeChange"
      />
      <el-button v-permission="['GET /admin/log/list']" class="filter-item" type="primary" icon="el-icon-search" @click="handleFilter">{{ $t('app.button.search') }}</el-button>
      <el-button class="filter-item" type="default" icon="el-icon-refresh" @click="handleReset">重置</el-button>
    </div>

    <!-- 查询结果 -->
    <el-table v-loading="listLoading" :data="list" :element-loading-text="$t('app.message.list_loading')" border fit highlight-current-row>
      <el-table-column align="center" :label="$t('sys_log.table.admin')" prop="admin" />
      <el-table-column align="center" :label="$t('sys_log.table.ip')" prop="ip" />
      <el-table-column align="center" :label="$t('sys_log.table.add_time')" prop="addTime" />
      <el-table-column align="center" :label="$t('sys_log.table.type')" prop="type">
        <template slot-scope="scope">
          <el-tag>{{ scope.row.type | typeFilter }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column align="center" :label="$t('sys_log.table.action')" prop="action" />
      <el-table-column align="center" :label="$t('sys_log.table.status')" prop="status">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status ? 'success' : 'error' ">{{ $t(scope.row.status ? 'sys_log.value.status_success' : 'sys_log.value.status_error') }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column align="center" :label="$t('sys_log.table.result')" prop="result" />
      <el-table-column align="center" :label="$t('sys_log.table.comment')" prop="comment" />

    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="listQuery.page" :limit.sync="listQuery.limit" @pagination="getList" />

  </div>
</template>

<script>
import { listLog } from '@/api/log'
import Pagination from '@/components/Pagination'

const typeMap = {
  0: '一般操作',
  1: '安全操作',
  2: '订单操作',
  3: '其他操作'
}

export default {
  name: 'Log',
  components: { Pagination },
  filters: {
    typeFilter(type) {
      return typeMap[type]
    }
  },
  data() {
    return {
      list: null,
      total: 0,
      listLoading: true,
      timeRange: null,
      listQuery: {
        page: 1,
        limit: 20,
        name: undefined,
        status: undefined,
        startTime: undefined,
        endTime: undefined,
        sort: 'add_time',
        order: 'desc'
      },
      pickerOptions: {
        shortcuts: [
          {
            text: '今天',
            onClick(picker) {
              const end = new Date()
              const start = new Date()
              start.setHours(0, 0, 0, 0)
              picker.$emit('pick', [start, end])
            }
          },
          {
            text: '最近一周',
            onClick(picker) {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
              picker.$emit('pick', [start, end])
            }
          },
          {
            text: '最近一个月',
            onClick(picker) {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
              picker.$emit('pick', [start, end])
            }
          },
          {
            text: '最近三个月',
            onClick(picker) {
              const end = new Date()
              const start = new Date()
              start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
              picker.$emit('pick', [start, end])
            }
          }
        ]
      },
      rules: {
        name: [
          { required: true, message: '角色名称不能为空', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.listLoading = true
      listLog(this.listQuery)
        .then(response => {
          this.list = response.data.data.list
          this.total = response.data.data.total
          this.listLoading = false
        })
        .catch(() => {
          this.list = []
          this.total = 0
          this.listLoading = false
        })
    },
    handleFilter() {
      this.listQuery.page = 1
      this.getList()
    },
    handleTimeChange(value) {
      if (value && value.length === 2) {
        this.listQuery.startTime = value[0]
        this.listQuery.endTime = value[1]
      } else {
        this.listQuery.startTime = undefined
        this.listQuery.endTime = undefined
      }
    },
    handleReset() {
      this.listQuery = {
        page: 1,
        limit: 20,
        name: undefined,
        status: undefined,
        startTime: undefined,
        endTime: undefined,
        sort: 'add_time',
        order: 'desc'
      }
      this.timeRange = null
      this.getList()
    }
  }
}
</script>
