# litemall 项目前端代码分析报告

litemall 的可视化实现分为 `litemall-admin` 管理后台与 `litemall-wx` 客户端小程序两条主线，二者都以“页面结构 → 状态存储 → 请求封装”的同心圆展开，其中后台依赖 Vue2 + Element-UI 并将接口集中在 `src/api/goods.js`，客户端使用微信小程序原生栈并通过 `utils/util.js` 携带 `X-Litemall-Token` 访问 `config/api.js` 内拼接好的 `/wx/goods/*`；因此后续所有功能描述都围绕各自页面的分块与数据流展开。

## 管理后台

| 管理后台功能 | 前端文件 | 主要分块 | 核心状态/数据 | 后端接口 |
| --- | --- | --- | --- | --- |
| 商品列表 | `src/views/goods/list.vue` | 筛选操作区、结果表格、分页、详情弹窗 | `listQuery/list/total/listLoading`、`batchDeleteArr` | `GET /admin/goods/list` |
| 商品详情 | `src/views/goods/list.vue`（`el-dialog`） | 只读画廊、关键属性、富文本 | `goodsDetail/detailDialogVisible/detailLoading` | `GET /admin/goods/detail` |
| 商品创建 | `src/views/goods/create.vue` | 基础信息、规格卡片、货品表、属性表、TinyMCE | `goods/specifications/products/attributes/uploadList` | `POST /admin/goods/create` |
| 商品更新 | `src/views/goods/edit.vue` | 与创建页同构并包含旧值缓存 | `goods/specifications/products/attributes/tempGoods` | `POST /admin/goods/update` |
| 商品删除 | `src/views/goods/list.vue` | 操作列、批量删除按钮 | `batchDeleteArr`、`deleteGoods(row)` | `POST /admin/goods/delete` |

### 商品列表

后台商品列表（`GET /admin/goods/list`）集中在 `litemall-admin/src/views/goods/list.vue`，其核心是围绕 `listQuery` 对象构建的“单一数据源查询”模式。模板由筛选面板、结果 `el-table`、分页组件与详情弹窗四块组成，脚本层在 `data()` 中维护 `listQuery`（分页、筛选、排序参数）、`list`、`total` 等状态。所有用户交互如筛选（`handleFilter`）或分页（`Pagination` 组件的 `@pagination` 回调）都通过修改 `listQuery` 并统一调用 `getList()`。当前模板尚未接入 `sort-change` 事件，真实代码中也没有 `handleSortChange` 方法，因此排序能力仍停留在占位配置；需要注意 `listQuery` 没有声明 `goodsId`，即便模板提供输入框也会被静默忽略，这是列表当前最明显的功能缺口。

```vue
data() {
  return {
    listQuery: {
      page: 1,
      limit: 20,
      goodsSn: undefined,
      name: undefined,
      sort: 'add_time',
      order: 'desc'
      // goodsId: undefined // TODO: 未声明导致 ID 筛选输入框始终不生效
    }
  }
}
```

### 商品详情

后台商品详情（`GET /admin/goods/detail`）在列表页以 `el-dialog` 打开，触发逻辑为 `handleDetail(row)` 设置 `id` 并调用 `detailGoods(row.id)`，接口返回的 `goods/specifications/products/attributes` 会被写进 `goodsDetail` 并以只读形式渲染画廊、属性、关键字等信息，这一设计对应后端“聚合四张表”的实现，前端只负责展示，但仍需在 `finally` 块中重置 `detailLoading` 以避免多次查看时残留骨架屏。

### 商品创建

后台商品创建（`POST /admin/goods/create`）由 `src/views/goods/create.vue` 完成，页面使用若干 `el-card` 将基础信息、规格参数、货品列表、属性列表与富文本详情分块呈现。其核心功能之一是 `specToProduct()` 方法，它通过一个纯粹的、自包含的算法逻辑动态生成SKU矩阵：首先遍历用户输入的规格，将同名规格（如“颜色”）的值聚合成分组；随后通过一个 `do-while` 循环和一组“指针”数组模拟多进制计数器，以此计算出所有规格的笛卡尔积；最终在每次循环中生成一个对应的货品（Product）对象。上传功能由 `createStorage(formData)` 负责，随表单提交的 `finalGoods` 在 `publishGoods(finalGoods)` 中被发送，若要提升准确性需在提交前补齐价格、库存的前端校验提示。

```js
specToProduct() {
  if (this.specifications.length === 0) return
  var specValues = [] // 将同名规格分组，供后续笛卡儿积使用
  var spec = this.specifications[0].specification
  var values = [0]
  for (var i = 1; i < this.specifications.length; i++) {
    const aspec = this.specifications[i].specification
    if (aspec === spec) {
      values.push(i)
    } else {
      specValues.push(values)
      spec = aspec
      values = [i]
    }
  }
  specValues.push(values)
  var productsIndex = 0
  var products = []
  var combination = []
  var n = specValues.length
  for (var s = 0; s < n; s++) {
    combination[s] = 0
  }
  var index = 0
  var isContinue = false
  do {
    var specifications = []
    for (var x = 0; x < n; x++) {
      var z = specValues[x][combination[x]]
      specifications.push(this.specifications[z].value)
    }
    products[productsIndex] = { id: productsIndex, specifications, price: 0.00, number: 0, url: '' }
    productsIndex++
    index++
    combination[n - 1] = index
    for (var j = n - 1; j >= 0; j--) {
      if (combination[j] >= specValues[j].length) {
        combination[j] = 0
        index = 0
        if (j - 1 >= 0) {
          combination[j - 1] = combination[j - 1] + 1
        }
      }
    }
    isContinue = false
    for (var p = 0; p < n; p++) {
      if (combination[p] !== 0) {
        isContinue = true // 只要还有指针不在起点，就继续遍历下一组规格
      }
    }
  } while (isContinue)
  this.products = products // 生成的 SKU 列表将随提交一起发送给后端
}
```

```js
uploadPicUrl(response) {
  this.goods.picUrl = response.data.url // 单图上传：直接回填封面
},
handleGalleryUrl(response) {
  if (response.errno === 0) this.goods.gallery.push(response.data.url)
},
handleRemove(file) {
  const url = file.response ? file.response.data.url : file.url
  this.goods.gallery = this.goods.gallery.filter(item => item !== url)
}
```

### 商品更新

后台商品更新（`POST /admin/goods/update`）由 `src/views/goods/edit.vue` 承担，流程是根据路由参数调用 `detailGoods(id)` 回填 Form，再在 `handleUpdate()` 中整合现有的 `goods/specifications/products/attributes` 并附带 `id/updateTime` 后调用 `editGoods(finalGoods)`；页面结构与 `create.vue` 基本一致，区别在于需要对比旧值以支持后端的“增量更新策略”，因此组件内保留了 `tempGoods` 以便在提交后决定是否刷新详情，而富文本、上传、规格组合等逻辑直接复用创建页。

### 商品删除

后台商品删除（`POST /admin/goods/delete`）仍在 `list.vue` 内实现，`handleDelete(row)` 通过 `this.$confirm` 获取用户确认后调用 `deleteGoods(row)`，成功则刷新列表，失败则读取 `res.data.errmsg` Toast 提示；目前请求体直接传入整行数据，包含 `detail` 等大字段，建议改为 `{ id: row.id }` 以减小 payload，同时批量删除 `handleBatchDelete()` 只是遍历选中项逐个调用删除接口，缺乏统一的成功/失败统计，可以考虑与后端协调新增批量接口或在前端汇总结果。

```js
handleDelete(row) {
  this.$confirm('确定删除?', '警告', { /* 省略弹窗配置 */ }).then(() => {
    deleteGoods(row).then(() => { // row 包含 detail 等大字段，网络与日志均会放大
      this.$notify.success({ title: '成功', message: '删除成功' })
      this.getList()
    }).catch(response => {
      this.$notify.error({ title: '失败', message: response.data.errmsg })
    })
  })
}
```

## 客户端

| 客户端功能 | 前端页面 | 页面结构 | 状态/数据 | 后端接口 |
| --- | --- | --- | --- | --- |
| 商品分类+列表 | `pages/category/category` | 横向分类栏 + 下拉宫格列表 | `navList/currentNav/goodsList/page/limit/pages` | `GET /wx/goods/category`、`GET /wx/goods/list` |
| 商品详情 | `pages/goods/goods` | 轮播、价格、规格弹窗、评论、参数、富文本、关联推荐 | `goods/attribute/specificationList/checkedSpecValues/productList/cartGoodsCount` | `GET /wx/goods/detail`、`GET /wx/goods/related` |
| 商品计数 | `pages/index/index` | 首页 banner + 模块化推荐 | `goodsCount/channel/newGoods/newHotBrand` | `GET /wx/goods/count`、`GET /wx/home/index` |

### 商品分类与列表

客户端商品列表（`GET /wx/goods/list`）位于 `litemall-wx/pages/category/category`，界面由顶部横向分类导航与下方滚动宫格组成。其数据加载流程与小程序生命周期紧密结合：页面 `onLoad` 时，从路由参数获取分类 `id` 并调用 `getCategoryInfo()`；该函数成功后会立即调用 `getGoodsList()`，形成“分类加载 → 商品加载”的链式数据流。当用户滚动触底触发 `onReachBottom` 时，会再次调用 `getGoodsList()` 并通过 `concat` 方法追加数据，实现无限滚动效果；由于 `util.request` 返回的 Promise 仅在 HTTP 失败时 reject，建议在 `then` 后对 `errno` 补充校验以免静默失败。

```js
getGoodsList() {
  util.request(api.GoodsList, { categoryId: this.data.id, page: this.data.page, limit: this.data.limit })
    .then(res => {
      if (res.errno !== 0) return wx.showToast({ title: res.errmsg, icon: 'none' }) // 建议增加该分支
      this.setData({
        goodsList: this.data.goodsList.concat(res.data.list),
        page: this.data.page + 1,
        pages: res.data.pages
      })
    })
}
```

### 商品详情

客户端商品详情（`GET /wx/goods/detail`）在 `pages/goods/goods` 渲染，页面包含轮播、价格区、规格入口等丰富模块。`getGoodsInfo(id)` 会从接口一次性获取所有数据并分发到页面，避免多次请求。规格切换的实现遵循了清晰的单向数据流：用户点击规格值触发 `clickSkuValue()`，该函数更新 `checkedSpecValues` 状态；接着调用 `getCheckedProductItem()` 遍历 `productList` 以查找与当前选中规格完全匹配的货品；最后将找到的货品的价格、库存等信息通过 `setData` 同步到视图，完成“用户交互 → 更新状态 → 查找数据 → 更新视图”的闭环。不过当前实现没有条件隐藏分享按钮，也未对接口错误进行 Toast 兜底。

```js
openShare() {
  if (!this.data.share) { // 后端 detail 接口会返回 share 配置
    return wx.showToast({ title: '暂不支持分享', icon: 'none' })
  }
  wx.showShareMenu({
    withShareTicket: true,
    menus: ['shareAppMessage', 'shareTimeline']
  })
}
```

### 商品数量统计

客户端商品数量统计（`GET /wx/goods/count`）出现在 `pages/index/index`，页面在 `getIndexData()` 并行调用首页聚合与计数接口后，将 `goodsCount` 显示在 banner 区域，同时把 `channel/newGoods/newHotBrand` 等数据灌入模块化组件；由于该接口只在 `onShow` 被调用，若想保证数据实时性，可以考虑在 `onPullDownRefresh` 时再次触发并等待 Promise 结束后调用 `wx.stopPullDownRefresh()`。

### 关联推荐

客户端关联推荐（`GET /wx/goods/related`）与详情页共存，`getGoodsRelated(id)` 把接口返回值写入 `relatedGoods` 并以横向滑动列表呈现，点击后跳转其他商品详情并重新触发 `getGoodsInfo`、`getGoodsRelated` 形成闭环；当前模板在列表为空时仍然占位，可通过 `relatedGoods.length > 0` 控制渲染，另外为了减轻接口压力，可在切换同一商品的规格时避免重复请求关联数据。

## 改进建议

1. **管理后台**  
   - 在 `listQuery` 中补充 `goodsId` 字段并接入 `el-table` 的 `sort-change` 事件，使 UI 中的“商品 ID”筛选与列排序真正生效。  
   - `handleDelete(row)` 与批量删除接口仅需 `id`，应当改为 `deleteGoods({ id: row.id })` 并在前端统计成功/失败数量，减少网络负载并提升可观测性。  
   - `create.vue/edit.vue` 可为价格、库存、货号等字段添加同步校验规则，并在 TinyMCE 上传失败时提供重试/错误文案，避免把错误完全推给后端。  
   - 规格生成算法完成后可校验 `products` 与 `specifications` 数量、自动填充默认价格，降低后端校验压力。

2. **客户端小程序**  
   - 统一在 `util.request` 之后判断 `errno`，并为 `category/goods/index` 等页面补充失败 Toast 或空态视图，这样后端业务错误不会被静默吞掉。  
   - `pages/goods/goods` 的分享入口应依据 `canShare` 字段展示/隐藏，同时给 `getGoodsInfo`、`getGoodsRelated` 等接口添加 loading/empty 态提示，提升可用性。  
   - 在分类列表的无限滚动中记录已加载页数和 `pages`，若返回列表为空或达到总页，及时提示并禁用继续加载，避免重复请求。  
   - 详情页可延迟加载评论、常见问题、关联推荐等非首屏模块，并缓存最近浏览的 `productList`，以提升体验与性能。

## 小结

综上，`list/detail/create/update/delete/count/related` 等核心接口在前端均有清晰的页面载体、状态字段与 API 封装：管理后台通过 `listQuery` 单源数据模式、表单化的 `GoodsAllinone` 结构以及统一的上传/规格生成逻辑完成商品的增删改查；客户端则以分类页、详情页、首页等模块化页面承接浏览与统计需求。要让这套映射更稳固，后台需要补齐 `goodsId` 与排序事件、将删除 payload 精简为 `{ id }` 并完善 SKU/价格的前端校验，客户端则应在所有 `util.request` 调用后处理 `errno`、按 `canShare/openShare` 控制分享入口、为关联与无限滚动提供空态与分页兜底。随着这些细节落地，本文档即可作为后续联调和代码审计的基线：任何接口改动都能迅速定位到对应的视图块与状态容器，前后端协作也更容易围绕同一数据契约展开。
