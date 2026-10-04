export const enumLabels = Object.freeze({
  ACTIVE: '启用', INACTIVE: '停用', DRAFT: '草稿', ARCHIVED: '已归档',
  OWNER_OCCUPIED: '业主自住', TENANT_OCCUPIED: '租户居住', AVAILABLE_FOR_RENT: '可出租', VACANT: '空置', UNKNOWN: '待确认',
  RESPONSIBLE: '主要居住人', MEMBER: '共同居住人', SELF: '本人', SPOUSE: '配偶', PARENT: '父母', CHILD: '子女', SIBLING: '兄弟姐妹', ROOMMATE: '合租人',
  PAID: '已缴清', PARTIALLY_PAID: '部分缴费', UNPAID: '未缴', OVERDUE: '已逾期',
  OPEN: '待处理', PROCESSING: '处理中', RESOLVED: '已解决',
  LEAK: '漏水', ELECTRICITY: '用电', GAS: '燃气', PUBLIC_FACILITY: '公共设施', NOISE: '噪音', OTHER: '其他',
  PUBLISHED: '挂牌中', RENTED: '已出租', CLOSED: '已关闭', EXPIRED: '已过期', AGENCY: '中介代理',
  PROPERTY_NOTICE: '物业通知', MAINTENANCE: '维修施工', BILLING: '缴费通知', EMERGENCY: '紧急通知',
  METERED: '计量收费', AREA: '按面积收费', FIXED: '固定收费', SUCCESS: '支付成功',
  STUDIO: '开间', ONE_BEDROOM: '一居室', ONE_BEDROOM_ONE_LIVING: '一室一厅', TWO_BEDROOM: '两居室', TWO_BEDROOM_ONE_LIVING: '两室一厅', TWO_BEDROOM_TWO_LIVING: '两室两厅', THREE_BEDROOM: '三居室', THREE_BEDROOM_ONE_LIVING: '三室一厅', THREE_BEDROOM_TWO_LIVING: '三室两厅', FOUR_BEDROOM: '四居室', OTHER: '其他户型',
  PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝', EXPIRING: '即将到期', REVOKED: '已撤销', YES: '可申请', NO: '暂不可申请', OWNERSHIP: '产权自动资格', RESIDENCE: '租赁入住资格'
})

export function displayEnum(value, fallback = '—') { if (value === null || value === undefined || value === '') return fallback; return enumLabels[value] || value }
export const displayLayoutType = value => displayEnum(value, '未填写')
export function statusTagType(value) { if (['ACTIVE', 'PAID', 'RESOLVED', 'OWNER_OCCUPIED', 'RENTED', 'SUCCESS', 'APPROVED', 'YES'].includes(value)) return 'success'; if (['OPEN', 'UNPAID', 'OVERDUE', 'EXPIRED', 'EMERGENCY', 'REJECTED', 'REVOKED', 'NO'].includes(value)) return 'danger'; if (['PROCESSING', 'PARTIALLY_PAID', 'AVAILABLE_FOR_RENT', 'DRAFT', 'BILLING', 'PENDING', 'EXPIRING'].includes(value)) return 'warning'; if (['TENANT_OCCUPIED', 'PUBLISHED', 'PROPERTY_NOTICE', 'MAINTENANCE'].includes(value)) return 'primary'; return 'info' }
export const formatMoney = value => new Intl.NumberFormat('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(value || 0))
export const formatMonth = (year, month) => `${year} 年 ${String(month).padStart(2, '0')} 月`
