<template>
  <div class="app-container">
    <div class="filter-container">
      <el-select
        v-model="filterForm.period"
        class="filter-item"
        style="width: 160px;"
        :placeholder="$t('stat_order_page.filter.period')"
        @change="handleFilter"
      >
        <el-option v-for="item in periodOptions" :key="item.value" :label="$t(item.labelKey)" :value="item.value" />
      </el-select>
      <el-select
        v-model="filterForm.categoryId"
        class="filter-item"
        style="width: 260px;"
        :loading="categoryLoading"
        :placeholder="$t('stat_order_page.filter.category')"
        @change="handleFilter"
      >
        <el-option :label="$t('stat_order_page.category.all')" :value="0" />
        <el-option-group v-for="group in categoryOptions" :key="group.id" :label="group.name">
          <el-option :label="group.name" :value="group.id" />
          <el-option
            v-for="child in group.children || []"
            :key="child.id"
            :label="`${group.name} / ${child.name}`"
            :value="child.id"
          />
        </el-option-group>
      </el-select>
      <el-button class="filter-item" type="primary" icon="el-icon-refresh" @click="handleFilter">{{ $t('app.button.search') }}</el-button>
    </div>
    <div class="chart-wrapper" v-loading="chartLoading">
      <ve-line :extend="chartExtend" :data="chartData" :settings="chartSettings" />
    </div>
  </div>
</template>

<script>
import { statOrder } from '@/api/stat'
import { listCategory } from '@/api/category'
import VeLine from 'v-charts/lib/line'
export default {
  components: { VeLine },
  data() {
    return {
      chartData: { columns: [], rows: [] },
      chartSettings: { labelMap: {} },
      chartExtend: {
        xAxis: { boundaryGap: true }
      },
      chartLoading: false,
      filterForm: {
        period: 'day',
        categoryId: 0
      },
      periodOptions: [
        { value: 'day', labelKey: 'stat_order_page.period.day' },
        { value: 'month', labelKey: 'stat_order_page.period.month' },
        { value: 'quarter', labelKey: 'stat_order_page.period.quarter' },
        { value: 'year', labelKey: 'stat_order_page.period.year' }
      ],
      categoryOptions: [],
      categoryLoading: false
    }
  },
  created() {
    this.updateLabelMap()
    this.loadCategories()
    this.fetchData()
  },
  watch: {
    '$i18n.locale'() {
      this.updateLabelMap()
    }
  },
  methods: {
    updateLabelMap() {
      this.chartSettings = {
        ...this.chartSettings,
        labelMap: {
          orders: this.$t('stat_order_page.chart.orders'),
          customers: this.$t('stat_order_page.chart.customers'),
          amount: this.$t('stat_order_page.chart.amount'),
          pcr: this.$t('stat_order_page.chart.pcr')
        }
      }
    },
    loadCategories() {
      this.categoryLoading = true
      listCategory()
        .then(response => {
          this.categoryOptions = response.data.data.list || []
        })
        .catch(() => {
          this.categoryOptions = []
        })
        .finally(() => {
          this.categoryLoading = false
        })
    },
    fetchData() {
      this.chartLoading = true
      statOrder({ ...this.filterForm })
        .then(response => {
          this.chartData = response.data.data
        })
        .finally(() => {
          this.chartLoading = false
        })
    },
    handleFilter() {
      this.fetchData()
    }
  }

}
</script>

<style scoped>
.chart-wrapper {
  margin-top: 20px;
}
</style>
