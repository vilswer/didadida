create table t_coin_record
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    user_id     bigint                             not null comment '投币用户ID',
    video_id    bigint                             not null comment '视频ID',
    coin_num    tinyint  default 1                 not null comment '投币数量（通常1或2）',
    create_time datetime default CURRENT_TIMESTAMP null comment '投币时间',
    constraint uk_user_video
        unique (user_id, video_id)
)
    comment '投币记录表' charset = utf8mb4;

create index idx_video_id
    on t_coin_record (video_id);

create table t_collection_folder
(
    folder_id   bigint auto_increment comment '收藏夹ID'
        primary key,
    user_id     bigint                                 not null comment '创建者用户ID',
    folder_name varchar(64)                            not null comment '收藏夹名称',
    description varchar(256) default ''                null comment '收藏夹描述',
    cover_url   varchar(512) default ''                null comment '收藏夹封面',
    is_public   tinyint      default 0                 null comment '是否公开：0-私密，1-公开',
    video_count int          default 0                 null comment '收藏视频数',
    create_time datetime     default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime     default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '收藏夹表' charset = utf8mb4;

create index idx_user_id
    on t_collection_folder (user_id);

create table t_collection_relation
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    folder_id   bigint                             not null comment '收藏夹ID',
    video_id    bigint                             not null comment '视频ID',
    create_time datetime default CURRENT_TIMESTAMP null comment '收藏时间',
    constraint uk_folder_video
        unique (folder_id, video_id)
)
    comment '收藏视频关联表' charset = utf8mb4;

create index idx_video_id
    on t_collection_relation (video_id);

create table t_comment
(
    comment_id    bigint auto_increment comment '评论ID'
        primary key,
    video_id      bigint                             not null comment '视频ID（分片键）',
    user_id       bigint                             not null comment '评论者ID',
    parent_id     bigint   default 0                 null comment '父评论ID（0表示一级评论）',
    reply_user_id bigint   default 0                 null comment '回复的用户ID（仅二级评论有效）',
    content       text                               not null comment '评论内容',
    like_count    bigint   default 0                 null comment '点赞数',
    reply_count   int      default 0                 null comment '回复数',
    status        tinyint  default 1                 null comment '状态：0-删除，1-正常',
    create_time   datetime default CURRENT_TIMESTAMP null comment '评论时间'
)
    comment '评论表' charset = utf8mb4;

create index idx_user_id
    on t_comment (user_id);

create index idx_video_parent
    on t_comment (video_id, parent_id);

create table t_danmaku
(
    danmaku_id  bigint auto_increment comment '弹幕ID'
        primary key,
    video_id    bigint                                not null comment '视频ID（分片键）',
    user_id     bigint                                not null comment '发送用户ID',
    content     varchar(256)                          not null comment '弹幕内容',
    video_time  decimal(10, 3)                        not null comment '在视频中的时间点（秒，精确到毫秒）',
    color       varchar(16) default '#FFFFFF'         null comment '弹幕颜色（HEX）',
    font_size   tinyint     default 25                null comment '字体大小',
    mode        tinyint     default 1                 null comment '弹幕模式：1-滚动，2-顶部，3-底部',
    create_time datetime    default CURRENT_TIMESTAMP null comment '发送时间'
)
    comment '弹幕表' charset = utf8mb4;

create index idx_video_time
    on t_danmaku (video_id, video_time);

create table t_like_record
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    user_id     bigint                             not null comment '点赞用户ID',
    target_type tinyint                            not null comment '点赞类型：1-视频，2-评论',
    target_id   bigint                             not null comment '点赞目标ID（视频ID或评论ID）',
    create_time datetime default CURRENT_TIMESTAMP null comment '点赞时间',
    constraint uk_user_target
        unique (user_id, target_type, target_id)
)
    comment '点赞记录表' charset = utf8mb4;

create table t_user
(
    user_id         bigint auto_increment comment '用户唯一ID'
        primary key,
    username        varchar(64)                            not null comment '登录用户名（唯一）',
    password        varchar(256)                           not null comment '加密后的密码（BCrypt）',
    nickname        varchar(64)                            not null comment '用户昵称',
    avatar          varchar(512) default ''                null comment '头像URL',
    gender          tinyint      default 0                 null comment '性别：0-保密，1-男，2-女',
    birthday        date                                   null comment '生日',
    sign            varchar(256) default ''                null comment '个性签名',
    phone           varchar(20)  default ''                null comment '手机号（加密存储）',
    email           varchar(128) default ''                null comment '邮箱',
    level           tinyint      default 1                 null comment '用户等级（Lv0-Lv6）',
    exp             bigint       default 0                 null comment '经验值',
    coins           int          default 0                 null comment '硬币数',
    vip_type        tinyint      default 0                 null comment '会员类型：0-普通，1-月度大会员，2-年度大会员',
    vip_expire_time datetime                               null comment '会员过期时间',
    status          tinyint      default 1                 null comment '账号状态：0-封禁，1-正常',
    create_time     datetime     default CURRENT_TIMESTAMP null comment '注册时间',
    update_time     datetime     default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_username
        unique (username)
)
    comment '用户主表' charset = utf8mb4;

create index idx_create_time
    on t_user (create_time);

create index idx_phone
    on t_user (phone);

create table t_user_follow
(
    id             bigint auto_increment comment '主键ID'
        primary key,
    user_id        bigint                             not null comment '关注者ID（粉丝）',
    follow_user_id bigint                             not null comment '被关注者ID（UP主）',
    create_time    datetime default CURRENT_TIMESTAMP null comment '关注时间',
    constraint uk_user_follow
        unique (user_id, follow_user_id)
)
    comment '用户关注表' charset = utf8mb4;

create index idx_follow_user_id
    on t_user_follow (follow_user_id);

create table t_video
(
    video_id      bigint auto_increment comment '视频唯一ID（AV号）'
        primary key,
    user_id       bigint                             not null comment 'UP主用户ID',
    category_id   bigint                             not null comment '分区ID',
    title         varchar(128)                       not null comment '视频标题',
    description   text                               null comment '视频简介',
    cover_url     varchar(512)                       not null comment '封面图URL',
    video_url     varchar(512)                       not null comment '视频播放地址（CDN）',
    duration      int                                not null comment '视频时长（秒）',
    view_count    bigint   default 0                 null comment '播放量',
    danmaku_count bigint   default 0                 null comment '弹幕数',
    like_count    bigint   default 0                 null comment '点赞数',
    coin_count    bigint   default 0                 null comment '投币数',
    collect_count bigint   default 0                 null comment '收藏数',
    share_count   bigint   default 0                 null comment '转发数',
    audit_status  tinyint  default 0                 null comment '审核状态：0-待审核，1-通过，2-拒绝',
    status        tinyint  default 1                 null comment '可见状态：0-下架，1-公开，2-仅自己可见',
    publish_time  datetime                           null comment '发布时间',
    create_time   datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time   datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '视频主表' charset = utf8mb4;

create index idx_category_id
    on t_video (category_id);

create index idx_publish_time
    on t_video (publish_time);

create index idx_user_id
    on t_video (user_id);

create index idx_view_count
    on t_video (view_count);

create table t_video_category
(
    category_id bigint auto_increment comment '分区ID'
        primary key,
    name        varchar(64)                            not null comment '分区名称',
    description varchar(256) default ''                null comment '分区描述',
    icon        varchar(512) default ''                null comment '分区图标URL',
    sort_order  int          default 0                 null comment '排序权重',
    status      tinyint      default 1                 null comment '状态：0-禁用，1-启用',
    create_time datetime     default CURRENT_TIMESTAMP null comment '创建时间'
)
    comment '视频分区表' charset = utf8mb4;

create table t_video_tag
(
    tag_id      bigint auto_increment comment '标签ID'
        primary key,
    tag_name    varchar(64)                        not null comment '标签名称',
    use_count   bigint   default 0                 null comment '使用次数',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    constraint uk_tag_name
        unique (tag_name)
)
    comment '视频标签表' charset = utf8mb4;

create table t_video_tag_relation
(
    id          bigint auto_increment comment '主键ID'
        primary key,
    video_id    bigint                             not null comment '视频ID',
    tag_id      bigint                             not null comment '标签ID',
    create_time datetime default CURRENT_TIMESTAMP null comment '关联时间',
    constraint uk_video_tag
        unique (video_id, tag_id)
)
    comment '视频标签关联表' charset = utf8mb4;

create index idx_tag_id
    on t_video_tag_relation (tag_id);


