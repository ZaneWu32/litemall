<template>
  <div class="app-container">
    <div class="filter-container">
      <el-form :inline="true" size="small">
        <el-form-item :label="$t('stat_comment_page.filter.categoryL1')">
          <el-select
            v-model="filters.categoryL1"
            class="filter-item"
            :placeholder="$t('stat_comment_page.category.placeholder')"
            @change="handleL1Change"
          >
            <el-option :label="$t('stat_comment_page.category.all')" :value="0" />
            <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('stat_comment_page.filter.categoryL2')">
          <el-select
            v-model="filters.categoryL2"
            class="filter-item"
            clearable
            :disabled="subCategoryOptions.length === 0"
            :placeholder="$t('stat_comment_page.category.child_placeholder')"
            @change="handleQuery"
          >
            <el-option v-for="item in subCategoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('stat_comment_page.filter.order')">
          <el-radio-group v-model="filters.order" size="small" @change="handleOrderChange">
            <el-radio-button label="desc">{{ $t('stat_comment_page.order.desc') }}</el-radio-button>
            <el-radio-button label="asc">{{ $t('stat_comment_page.order.asc') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">{{ $t('app.button.search') }}</el-button>
        </el-form-item>
      </el-form>
    </div>
    <el-table
      v-loading="listLoading"
      :data="list"
      border
      fit
      highlight-current-row
      :empty-text="$t('stat_comment_page.message.empty')"
      @sort-change="handleSortChange"
      :default-sort="{ prop: 'avgStar', order: 'descending' }"
    >
      <el-table-column prop="goodsId" :label="$t('stat_comment_page.table.goodsId')" width="120" align="center" />
      <el-table-column prop="goodsName" :label="$t('stat_comment_page.table.goodsName')" min-width="200" show-overflow-tooltip />
      <el-table-column prop="categoryName" :label="$t('stat_comment_page.table.categoryName')" min-width="180" show-overflow-tooltip />
      <el-table-column
        prop="avgStar"
        :label="$t('stat_comment_page.table.avgStar')"
        width="160"
        sortable="custom"
        align="center"
      >
        <template slot-scope="scope">
          {{ formatAverage(scope.row.avgStar) }}
        </template>
      </el-table-column>
      <el-table-column prop="userCount" :label="$t('stat_comment_page.table.userCount')" width="160" align="center" />
    </el-table>
  </div>
</template>

<script>
import { statComment } from '@/api/stat'
import { listCatAndBrand } from '@/api/goods'

export default {
  name: 'StatComment',
  data() {
    return {
      listLoading: false,
      list: [],
      filters: {
        categoryL1: 0,
        categoryL2: null,
        order: 'desc'
      },
      categoryOptions: [],
      subCategoryOptions: []
    }
  },
  created() {
    this.fetchCategories()
    this.getList()
  },
  methods: {
    fetchCategories() {
      listCatAndBrand()
        .then(response => {
          const data = response.data.data || {}
          this.categoryOptions = data.categoryList || []
          this.refreshSubCategories()
        })
        .catch(() => {
          this.categoryOptions = []
          this.subCategoryOptions = []
        })
    },
    refreshSubCategories() {
      if (!this.filters.categoryL1 || this.filters.categoryL1 === 0) {
        this.subCategoryOptions = []
        this.filters.categoryL2 = null
        return
      }
      const target = this.categoryOptions.find(item => item.value === this.filters.categoryL1)
      this.subCategoryOptions = (target && target.children) ? target.children : []
      if (this.subCategoryOptions.length === 0) {
        this.filters.categoryL2 = null
      }
    },
    handleL1Change(value) {
      this.filters.categoryL1 = value || 0
      this.filters.categoryL2 = null
      this.refreshSubCategories()
      this.handleQuery()
    },
    handleOrderChange() {
      this.handleQuery()
    },
    handleSortChange({ prop, order }) {
      if (prop !== 'avgStar') {
        return
      }
      if (order === 'ascending') {
        if (this.filters.order !== 'asc') {
          this.filters.order = 'asc'
          this.getList()
        }
      } else if (order === 'descending') {
        if (this.filters.order !== 'desc') {
          this.filters.order = 'desc'
          this.getList()
        }
      } else if (this.filters.order !== 'desc') {
        this.filters.order = 'desc'
        this.getList()
      }
    },
    handleQuery() {
      this.getList()
    },
    formatAverage(value) {
      if (value === null || value === undefined) {
        return '-'
      }
      return Number(value).toFixed(2)
    },
    getList() {
      this.listLoading = true
      const query = {
        categoryId: this.filters.categoryL2 || this.filters.categoryL1 || 0,
        order: this.filters.order
      }
      statComment(query)
        .then(response => {
          const data = response.data.data || {}
          this.list = data.rows || []
        })
        .catch(() => {
          this.list = []
        })
        .finally(() => {
          this.listLoading = false
        })
    }
  }
}
</script>
