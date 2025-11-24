<template>
  <div class="app-container">

    <!-- 查询和其他操作 -->
    <div class="filter-container">
      <el-input v-model="listQuery.keyword" clearable class="filter-item" style="width: 260px;" :placeholder="$t('goods_comment.placeholder.filter_keyword')" @keyup.enter.native="handleFilter" />
      <el-button class="filter-item" type="primary" icon="el-icon-search" @click="handleFilter">{{ $t('app.button.search') }}</el-button>
      <el-button :loading="downloadLoading" class="filter-item" type="primary" icon="el-icon-download" @click="handleDownload">{{ $t('app.button.download') }}</el-button>
    </div>

    <!-- 查询结果 -->
    <el-table v-loading="listLoading" :data="list" :element-loading-text="$t('app.message.list_loading')" border fit highlight-current-row>

      <el-table-column align="center" :label="$t('goods_comment.table.user_id')" prop="userId" />

      <el-table-column align="center" :label="$t('goods_comment.table.value_id')" prop="valueId" />

      <el-table-column align="center" min-width="160px" :label="$t('goods_comment.table.goods_name')" prop="goodsName" />

      <el-table-column align="center" min-width="140px" :label="$t('goods_comment.table.category_name')" prop="categoryName" />

      <el-table-column align="center" :label="$t('goods_comment.table.star')" prop="star" sortable />

      <el-table-column align="center" :label="$t('goods_comment.table.content')" prop="content" />

      <el-table-column align="center" :label="$t('goods_comment.table.add_time')" prop="addTime">
        <template slot-scope="scope">
          {{ formatDate(scope.row.addTime) }}
        </template>
      </el-table-column>

      <el-table-column align="center" :label="$t('goods_comment.table.actions')" width="200" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button type="primary" size="mini" @click="handleReply(scope.row)">{{ $t('app.button.reply') }}</el-button>
          <el-button type="danger" size="mini" @click="handleDelete(scope.row)">{{ $t('app.button.delete') }}</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="listQuery.page" :limit.sync="listQuery.limit" @pagination="getList" />

    <!-- 评论回复 -->
    <el-dialog :visible.sync="replyFormVisible" :title="$t('goods_comment.dialog.reply')">
      <el-form ref="replyForm" :model="replyForm" status-icon label-position="left" label-width="100px" style="width: 400px; margin-left:50px;">
        <el-form-item :label="$t('goods_comment.form.content')" prop="content">
          <el-input v-model="replyForm.content" :autosize="{ minRows: 4, maxRows: 8}" type="textarea" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="replyFormVisible = false">{{ $t('app.button.cancel') }}</el-button>
        <el-button type="primary" @click="reply">{{ $t('app.button.confirm') }}</el-button>
      </div>
    </el-dialog>

  </div>
</template>

<script>
import { listComment, deleteComment } from '@/api/comment'
import { replyComment } from '@/api/order'
import Pagination from '@/components/Pagination' // Secondary package based on el-pagination

export default {
  name: 'Comment',
  components: { Pagination },
  data() {
    return {
      list: [],
      total: 0,
      listLoading: true,
      listQuery: {
        page: 1,
        limit: 20,
        keyword: undefined,
        sort: 'add_time',
        order: 'desc'
      },
      downloadLoading: false,
      replyForm: {
        commentId: 0,
        content: ''
      },
      replyFormVisible: false
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.listLoading = true
      listComment(this.listQuery).then(response => {
        this.list = response.data.data.list
        this.total = response.data.data.total
        this.listLoading = false
      }).catch(() => {
        this.list = []
        this.total = 0
        this.listLoading = false
      })
    },
    handleFilter() {
      this.listQuery.page = 1
      this.getList()
    },
    handleReply(row) {
      this.replyForm = { commentId: row.id, content: '' }
      this.replyFormVisible = true
    },
    reply() {
      replyComment(this.replyForm).then(response => {
        this.replyFormVisible = false
        this.$notify.success({
          title: '成功',
          message: '回复成功'
        })
      }).catch(response => {
        this.$notify.error({
          title: '失败',
          message: response.data.errmsg
        })
      })
    },
    handleDelete(row) {
      deleteComment(row).then(response => {
        this.$notify({
          title: '成功',
          message: '删除成功',
          type: 'success',
          duration: 2000
        })
        this.getList()
      })
    },
    handleDownload() {
      this.downloadLoading = true
      import('@/vendor/Export2Excel').then(excel => {
        const tHeader = ['评论ID', '用户ID', '商品ID', '商品名称', '商品类别', '评分', '评论内容', '评论日期']
        const filterVal = ['id', 'userId', 'valueId', 'goodsName', 'categoryName', 'star', 'content', 'addTime']
        const formatted = this.list.map(item => ({
          ...item,
          addTime: this.formatDate(item.addTime)
        }))
        excel.export_json_to_excel2(tHeader, formatted, filterVal, '商品评论信息')
        this.downloadLoading = false
      })
    },
    formatDate(value) {
      if (!value) {
        return '-'
      }
      return value.toString().slice(0, 10)
    }
  }
}
</script>
