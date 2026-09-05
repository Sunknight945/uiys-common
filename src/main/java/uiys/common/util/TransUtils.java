package uiys.common.util;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uiys.common.exception.BusinessException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 特殊转化工具
 */
public class TransUtils {


    private static final Logger logger = LoggerFactory.getLogger(TransUtils.class);

    private TransUtils() {
    }

    private static volatile TransUtils INSTANCE;

    // 全局缓存：全类名 -> 表名
    private final ConcurrentHashMap<String, String> classTableTrans = new ConcurrentHashMap<>(10);

    // 私有内部获取原始Map，外部不可调用
    private ConcurrentHashMap<String, String> getClassTableTrans() {
        return classTableTrans;
    }

    // 对外提供只读视图，禁止外部修改缓存
    public Map<String, String> getClassNameTable() {
        return Collections.unmodifiableMap(this.getClassTableTrans());
    }

    public static TransUtils getInstance() {
        if (Objects.isNull(INSTANCE)) {
            synchronized (TransUtils.class) {
                if (Objects.isNull(INSTANCE)) {
                    INSTANCE = new TransUtils();
                }
            }
        }
        return INSTANCE;
    }

    // 唯一合法写入入口
    public void setClassNameTable(String classFullName, String tableName) {
        if (Objects.nonNull(classFullName) && Objects.nonNull(tableName)) {
            classTableTrans.put(classFullName, tableName);
        }
    }

    public String getTableFromClassName(String classFullName) {
        if (Objects.isNull(classFullName)) {
            return null;
        }
        return classTableTrans.getOrDefault(classFullName, classFullName);
    }

    public static void main(String[] args) {
        TransUtils util = TransUtils.getInstance();
        // 正确存入：调用封装方法，不直接操作Map
        util.setClassNameTable("nihc", "uide");
        // 正确读取
        String table = util.getTableFromClassName("nihc");
        System.out.println(table);
    }


    public static <E, T, EF, R> List<T> filterTrans(List<E> big, List<T> small, Function<E, EF> t1, Function<T, EF> t2, Function<E, R> t3, Function<R, T> tr) {
        if (big.isEmpty() || small.isEmpty()) {
            return null;
        }
        List<EF> smallContent = small.stream().map(t2).distinct().toList();
        List<E> bigRemain = big.stream().filter(item -> smallContent.contains(t1.apply(item))).toList();
        return bigRemain.stream().map(t3).map(tr).toList();
    }

    public static <E, T, R> List<R> filterTrans(List<E> big, List<T> small, Function<E, R> f1, Function<T, R> f2, Function<E, R> fr) {
        if (big.isEmpty() || small.isEmpty()) {
            return null;
        }
        List<R> smallContent = small.stream().map(f2).distinct().toList();
        List<E> bigRemain = big.stream().filter(item -> smallContent.contains(f1.apply(item))).toList();
        return bigRemain.stream().map(fr).toList();
    }

    /**
     * 过滤已存在数据并转换实体
     *
     * @param entityList     待写入原始实体集合E（数据库映射实体，带@GenFromEntity）
     * @param dbExistList    数据库已存在数据T
     * @param getEntityKey   原始实体获取唯一标识EF
     * @param getDbKey       库中数据获取唯一标识EF
     * @param entityToMid    E转中间对象R
     * @param midToTarget    R转最终保存对象T
     * @param targetConsumer 转换后对象回调处理
     * @return 过滤+转换后的待入库集合
     */
    public static <E, T, EF, R> List<T> filterExistTrans(List<E> entityList, List<T> dbExistList, Function<E, EF> getEntityKey, Function<T, EF> getDbKey, Function<E, R> entityToMid, Function<R, T> midToTarget, Consumer<T> targetConsumer) {
        // 待写入集合为空，直接返回
        if (entityList.isEmpty()) {
            logger.info("[集合类型:{}] 待写入数据为空列表，直接返回null", entityList.getClass().getSimpleName());
            return null;
        }

        List<T> result = null;
        // 统一从原始实体获取表名（运维/统计唯一依据）

        E first = entityList.getFirst();
        List<E> list = Lists.newArrayList(first);
        List<T> list1 = list.stream().map(entityToMid).map(midToTarget).toList();
        String entityClassName = list1.getFirst().getClass().getName();
        String tableName = TransUtils.getInstance().getTableFromClassName(entityClassName);
        try {
            // 兼容dbExistList为null，避免stream空指针
            Set<EF> existKeySet;
            if (dbExistList != null && !dbExistList.isEmpty()) {
                existKeySet = dbExistList.stream().map(getDbKey).collect(Collectors.toSet());
            } else {
                existKeySet = new HashSet<>();
            }

            logger.info("[表名:{}] 开始重复数据过滤，待处理总条数：{}，库中已存在数据条数：{}", tableName, entityList.size(), existKeySet.size());

            // 过滤掉数据库已存在记录
            List<E> needInsertEntities = entityList.stream().filter(item -> !existKeySet.contains(getEntityKey.apply(item))).toList();

            // 实体多层转换 + 回调填充
            result = needInsertEntities.stream().map(entityToMid).map(midToTarget).peek(targetConsumer).collect(Collectors.toList());

            logger.info("[表名:{}] 重复数据过滤完成，最终待入库条数：{}", tableName, result.size());

        } catch (Exception e) {
            // 异常日志带上表名，方便运维定位哪张表报错；不序列化大集合防止OOM
            String errMsg = String.format("[表名:%s] 数据转化异常，待写入集合长度：%s，库中已有数据长度：%s", tableName, entityList.size(), dbExistList == null ? 0 : dbExistList.size());
            throw new BusinessException(errMsg, e);
        }
        return result;
    }


    /**
     * 过滤已存在数据并转换实体
     *
     * @param dtoList             待写入原始实体集合E（数据库映射实体，带@GenFromEntity）
     * @param dbExistList         数据库已存在数据T
     * @param getEntityKey        原始实体获取唯一标识EF
     * @param getDbKey            库中数据获取唯一标识EF
     * @param converterDot2Entity E转中间对象R
     * @param targetConsumer      转换后对象回调处理
     * @return 过滤+转换后的待入库集合
     */
    public static <E, T, EF> List<T> createFilter(List<E> dtoList,
                                                  List<T> dbExistList,
                                                  Function<E, EF> getEntityKey,
                                                  Function<T, EF> getDbKey,
                                                  Function<E, T> converterDot2Entity,
                                                  Consumer<T> targetConsumer) {
        // 待写入集合为空，直接返回
        if (dtoList.isEmpty()) {
            logger.info("[集合类型:{}] 待写入数据为空列表，直接返回null", dtoList.getClass().getSimpleName());
            return null;
        }

        List<T> result = null;
        // 统一从原始实体获取表名（运维/统计唯一依据）

        E first = dtoList.getFirst();
        List<E> list = Lists.newArrayList(first);
        List<T> entityItem = list.stream().map(converterDot2Entity).toList();
        String entityClassName = entityItem.getFirst().getClass().getName();
        String tableName = TransUtils.getInstance().getTableFromClassName(entityClassName);
        try {
            // 兼容dbExistList为null，避免stream空指针
            Set<EF> existKeySet;
            if (dbExistList != null && !dbExistList.isEmpty()) {
                existKeySet = dbExistList.stream().map(getDbKey).collect(Collectors.toSet());
            } else {
                existKeySet = new HashSet<>();
            }

            logger.info("[表名:{}] 开始重复数据过滤，待处理总条数：{}，库中已存在数据条数：{}", tableName, dtoList.size(), existKeySet.size());

            // 过滤掉数据库已存在记录
            List<E> needInsertEntities = dtoList.stream().filter(item -> !existKeySet.contains(getEntityKey.apply(item))).toList();

            // 实体多层转换 + 回调填充
            result = needInsertEntities.stream().map(converterDot2Entity).peek(targetConsumer).collect(Collectors.toList());

            logger.info("[表名:{}] 重复数据过滤完成，最终待入库条数：{}", tableName, result.size());

        } catch (Exception e) {
            // 异常日志带上表名，方便运维定位哪张表报错；不序列化大集合防止OOM
            String errMsg = String.format("[表名:%s] 数据转化异常，待写入集合长度：%s，库中已有数据长度：%s", tableName, dtoList.size(), dbExistList == null ? 0 : dbExistList.size());
            throw new BusinessException(errMsg, e);
        }
        return result;
    }


    /**
     * 过滤已存在数据并转换实体
     *
     * @param dtoList             待写入原始实体集合E（数据库映射实体，带@GenFromEntity）
     * @param dbExistList         数据库已存在数据T
     * @param getEntityKey        原始实体获取唯一标识EF
     * @param getDbKey            库中数据获取唯一标识EF
     * @param converterDot2Entity E转中间对象R
     * @param targetConsumer      转换后对象回调处理
     * @return 过滤+转换后的待入库集合
     */
    public static <E, T, EF> List<T> updateFilter(List<E> dtoList,
                                                  List<T> dbExistList,
                                                  Function<E, EF> getEntityKey,
                                                  Function<T, EF> getDbKey,
                                                  Function<E, T> converterDot2Entity,
                                                  Function<E, EF> getDtoVersion,
                                                  Function<T, EF> getEntityVersion,
                                                  Consumer<T> targetConsumer) {
        // 待写入集合为空，直接返回
        if (dtoList.isEmpty()) {
            logger.info("[集合类型:{}] 待写入数据为空列表，直接返回null", dtoList.getClass().getSimpleName());
            return null;
        }

        List<T> result = null;
        // 统一从原始实体获取表名（运维/统计唯一依据）

        E first = dtoList.getFirst();
        List<E> list = Lists.newArrayList(first);
        List<T> entityItem = list.stream().map(converterDot2Entity).toList();
        String entityClassName = entityItem.getFirst().getClass().getName();
        String tableName = TransUtils.getInstance().getTableFromClassName(entityClassName);
        try {
            // 兼容dbExistList为null，避免stream空指针
            Map<EF, T> existKeySet;
            if (dbExistList != null && !dbExistList.isEmpty()) {
                existKeySet = dbExistList.stream().collect(Collectors.toMap(getDbKey, Function.identity(), (o1, o2) -> o1));
            } else {
                existKeySet = new HashMap<>();
            }

            logger.info("[表名:{}] 开始重复数据过滤，待处理总条数：{}，库中已存在数据条数：{}", tableName, dtoList.size(), existKeySet.size());

            // 过滤掉数据库已存在记录
            List<E> needInsertEntities = dtoList.stream().filter(item -> (existKeySet.containsKey(getEntityKey.apply(item))
                    && Objects.equals(getDtoVersion.apply(item), getEntityVersion.apply(existKeySet.get(getEntityKey.apply(item)))))).toList();

            // 实体多层转换 + 回调填充
            result = needInsertEntities.stream().map(converterDot2Entity).peek(targetConsumer).collect(Collectors.toList());

            logger.info("[表名:{}] 重复数据过滤完成，最终待入库条数：{}", tableName, result.size());

        } catch (Exception e) {
            // 异常日志带上表名，方便运维定位哪张表报错；不序列化大集合防止OOM
            String errMsg = String.format("[表名:%s] 数据转化异常，待写入集合长度：%s，库中已有数据长度：%s", tableName, dtoList.size(), dbExistList == null ? 0 : dbExistList.size());
            throw new BusinessException(errMsg, e);
        }
        return result;
    }


    // 剔除不存在
    public static <E, T> List<E> filterNotExist(List<E> big, List<T> small, Function<T, E> t1) {
        if (big.isEmpty()) {
            return null;
        }
        List<E> smallContent = small.stream().map(t1).distinct().toList();

        return big.stream().filter(smallContent::contains).toList();

    }

}
