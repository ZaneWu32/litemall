<template>
  <div class="app-container">
    <div class="filter-container">
      <el-form :inline="true" size="small">
        <el-form-item :label="$t('stat_comment_wordcloud_page.filter.goodsId')">
          <el-input v-model="filters.goodsId" clearable class="filter-item" style="width: 180px" :placeholder="$t('stat_comment_wordcloud_page.placeholder.goodsId')" @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item :label="$t('stat_comment_wordcloud_page.filter.categoryL1')">
          <el-select v-model="filters.categoryL1" class="filter-item" :placeholder="$t('stat_comment_wordcloud_page.placeholder.categoryL1')" @change="handleL1Change">
            <el-option :label="$t('stat_comment_wordcloud_page.category.all')" :value="0" />
            <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('stat_comment_wordcloud_page.filter.categoryL2')">
          <el-select v-model="filters.categoryL2" class="filter-item" clearable :disabled="subCategoryOptions.length === 0" :placeholder="$t('stat_comment_wordcloud_page.placeholder.categoryL2')" @change="handleQuery">
            <el-option v-for="item in subCategoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('stat_comment_wordcloud_page.filter.sample')">
          <el-input-number v-model="filters.sample" :min="50" :max="2000" :step="50" @change="handleQuery" />
        </el-form-item>
        <el-form-item :label="$t('stat_comment_wordcloud_page.filter.top')">
          <el-input-number v-model="filters.top" :min="10" :max="200" :step="10" @change="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">{{ $t('app.button.search') }}</el-button>
          <el-button icon="el-icon-refresh" @click="resetFilters">{{ $t('stat_comment_wordcloud_page.button.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="chart-wrapper" v-loading="loading">
      <ve-wordcloud
        :data="chartData"
        :settings="chartSettings"
        :extend="chartExtend"
        :judge-width="false"
        height="500px"
      />
    </div>
  </div>
</template>

<script>
import VeWordcloud from 'v-charts/lib/wordcloud'
import { statCommentWordcloud } from '@/api/stat'
import { listCatAndBrand } from '@/api/goods'

export default {
  name: 'StatCommentWordcloud',
  components: { VeWordcloud },
  data() {
    return {
      loading: false,
      chartData: {
        columns: ['name', 'value'],
        rows: []
      },
      chartSettings: {
        shape: 'circle',
        sizeRange: [12, 60]
      },
      chartExtend: {
        series: {
          gridSize: 8,
          rotationRange: [-45, 90],
          textStyle: {
            normal: {
              fontFamily: 'sans-serif'
            }
          }
        }
      },
      filters: {
        goodsId: '',
        categoryL1: 0,
        categoryL2: null,
        sample: 500,
        top: 50
      },
      categoryOptions: [],
      subCategoryOptions: []
    }
  },
  created() {
    this.fetchCategories()
    this.getData()
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
      const match = this.categoryOptions.find(item => item.value === this.filters.categoryL1)
      this.subCategoryOptions = match && match.children ? match.children : []
      if (!this.subCategoryOptions.length) {
        this.filters.categoryL2 = null
      }
    },
    handleL1Change(value) {
      this.filters.categoryL1 = value || 0
      this.filters.categoryL2 = null
      this.refreshSubCategories()
      this.handleQuery()
    },
    resetFilters() {
      this.filters = {
        goodsId: '',
        categoryL1: 0,
        categoryL2: null,
        sample: 500,
        top: 50
      }
      this.subCategoryOptions = []
      this.handleQuery()
    },
    handleQuery() {
      this.getData()
    },
    buildQuery() {
      const goodsId = this.filters.goodsId && Number(this.filters.goodsId)
      return {
        goodsId: goodsId && goodsId > 0 ? goodsId : undefined,
        categoryId: this.filters.categoryL2 || this.filters.categoryL1 || 0,
        sample: this.filters.sample,
        top: this.filters.top
      }
    },
    getData() {
      this.loading = true
      statCommentWordcloud(this.buildQuery())
        .then(response => {
          const data = response.data.data || []
          this.chartData = {
            columns: ['name', 'value'],
            rows: data
          }
        })
        .catch(() => {
          this.chartData = {
            columns: ['name', 'value'],
            rows: []
          }
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>

<style scoped>
.chart-wrapper {
  width: 100%;
  min-height: 520px;
}
</style>
