package me.chaopeng.utils;

import java.util.*;

/**
 * SortedSet - 跳表仿redis的实现
 * <p/>
 * 只支持score:Long key:Long
 * <p/>
 * 这里的 skiplist 实现和 William Pugh 在 "Skip Lists: A Probabilistic
 * Alternative to Balanced Trees" 里描述的差不多，只有三个地方进行了修改：
 * <p/>
 * <ul>
 * <li>这个实现允许重复值</li>
 * <li>不仅对 score 进行比对，还需要对 key 进行比对</li>
 * <li>每个节点都带有一个前驱指针，用于从表尾向表头迭代</li>
 * </ul>
 *
 * @author chao
 * @version 1.0 - 2014-04-06
 */
public final class SortedSet {

	public static final class RecordObject {
		private long score;
		private long key;
		private int rank;

		public RecordObject(long key, long score) {
			this.key = key;
			this.score = score;
		}

		public RecordObject(long score, long key, int rank) {
			this.score = score;
			this.key = key;
			this.rank = rank;
		}

		public long getScore() {
			return score;
		}

		public long getKey() {
			return key;
		}

		public int getRank() {
			return rank;
		}

		public void setRank(int rank) {
			this.rank = rank;
		}

		@Override
		public String toString() {
			return "RecordObject{" +
					"score=" + score +
					", key=" + key +
					", rank=" + rank +
					'}';
		}
	}

	public static final class RangeSpec {
		boolean minex, maxex;
		long min, max;

		public RangeSpec(boolean minex, boolean maxex, long min, long max) {
			this.minex = minex;
			this.maxex = maxex;
			this.min = min;
			this.max = max;
		}

		/**
		 * 默认为双闭区间
		 * @param min
		 * @param max
		 */
		public RangeSpec(long min, long max) {
			this.min = min;
			this.max = max;
			this.minex = false;
			this.maxex = false;
		}
	}

	private static final class SkipListLevel {
		/**
		 * 前进指针
		 */
		private SkipListNode forward = null;
		/**
		 * 这个层跨越的节点数量
		 */
		private int span = 0;
	}

	private static final class SkipListNode {
		/**
		 * 分值
		 */
		private Long score;
		/**
		 * 对象
		 */
		private Long obj;
		/**
		 * 后退指针
		 */
		private SkipListNode backward = null;
		/**
		 * 层
		 */
		private SkipListLevel[] level;

		public SkipListNode(int level, Long score, Long obj) {
			this.level = new SkipListLevel[level];
			for (int i = 0; i < level; i++) {
				this.level[i] = new SkipListLevel();
			}
			this.score = score;
			this.obj = obj;
		}

		public RecordObject toRecordObject() {
			return new RecordObject(obj, score);
		}
	}

	private static final class SkipList {
		private SkipListNode header = null;
		private SkipListNode tail = null;
		private int length = 0;
		private int level = 1;
		private Random random = new Random();

		private SkipList() {
			this.header = new SkipListNode(ZSKIPLIST_MAXLEVEL, 0L, null);
			for (int i = 0; i < this.header.level.length; i++) {
				this.header.level[i] = new SkipListLevel();
			}
		}

		/**
		 * 通过多次随机过程
		 *
		 * @return 一个介于 1 和 ZSKIPLIST_MAXLEVEL 之间的随机值，作为节点的层数。
		 */
		private int randomLevel() {
			int level = 1;
			while ((random.nextInt() & 0xFFFF) < (ZSKIPLIST_P * 0xFFFF))
				level += 1;
			return (level < ZSKIPLIST_MAXLEVEL) ? level : ZSKIPLIST_MAXLEVEL;
		}

		/**
		 * 将包含给定 score 的对象 obj 添加到 skiplist 里
		 * <p/>
		 * T_worst = O(N), T_average = O(log N)
		 */
		private SkipListNode insert(long score, long obj) {

			/**记录寻找元素过程中，每层能到达的最右节点*/
			SkipListNode[] update = new SkipListNode[ZSKIPLIST_MAXLEVEL];

			/**记录寻找元素过程中，每层所跨越的节点数*/
			int[] rank = new int[ZSKIPLIST_MAXLEVEL];

			SkipListNode x = this.header;

			int i;

			// 记录沿途访问的节点，并计数 span 等属性
			for (i = this.level - 1; i >= 0; i--) {
				rank[i] = i == (this.level - 1) ? 0 : rank[i + 1];

				// 右节点不为空
				while (x.level[i].forward != null &&
						// 右节点的 score 比给定 score 小
						(x.level[i].forward.score < score ||
								// 右节点的 score 相同，但节点的 member 比输入 member 要小
								(x.level[i].forward.score == score && x.level[i].forward.obj < obj))) {

					// 记录跨越了多少个元素
					rank[i] += x.level[i].span;
					// 继续向右前进
					x = x.level[i].forward;
				}
				// 保存访问节点
				update[i] = x;
			}

			// 因为这个函数不可能处理两个元素的 member 和 score 都相同的情况，
			// 所以直接创建新节点，不用检查存在性

			// 计算新的随机层数
			int level = randomLevel();

			// 如果 level 比当前 skiplist 的最大层数还要大
			// 那么更新 this.level 参数
			// 并且初始化 update 和 rank 参数在相应的层的数据
			if (level > this.level) {
				for (i = this.level; i < level; i++) {
					rank[i] = 0;
					update[i] = this.header;
					update[i].level[i].span = this.length;
				}
				this.level = level;
			}

			// 创建新节点
			x = new SkipListNode(level, score, obj);

			// 根据 update 和 rank 两个数组的资料，初始化新节点
			// 并设置相应的指针
			// O(N)
			for (i = 0; i < level; i++) {
				// 设置指针
				x.level[i].forward = update[i].level[i].forward;
				update[i].level[i].forward = x;

				// 设置 span
				x.level[i].span = update[i].level[i].span - (rank[0] - rank[i]);
				update[i].level[i].span = (rank[0] - rank[i]) + 1;
			}

			// 更新沿途访问节点的 span 值
			for (i = level; i < this.level; i++) {
				update[i].level[i].span++;
			}

			// 设置后退指针
			x.backward = (update[0] == this.header) ? null : update[0];
			// 设置 x 的前进指针
			if (x.level[0].forward != null)
				x.level[0].forward.backward = x;
			else
				// 这个是新的表尾节点
				this.tail = x;

			// 更新跳跃表节点数量
			this.length++;

			return x;
		}

		/**
		 * 节点删除函数
		 * <p/>
		 * T = O(N)
		 */
		private void deleteNode(SkipListNode x, SkipListNode[] update) {
			int i;

			// 修改相应的指针和 span , O(N)
			for (i = 0; i < this.level; i++) {
				if (update[i].level[i].forward == x) {
					update[i].level[i].span += x.level[i].span - 1;
					update[i].level[i].forward = x.level[i].forward;
				} else {
					update[i].level[i].span -= 1;
				}
			}

			// 处理表头和表尾节点
			if (x.level[0].forward != null) {
				x.level[0].forward.backward = x.backward;
			} else {
				this.tail = x.backward;
			}

			// 收缩 level 的值, O(N)
			while (this.level > 1 && this.header.level[this.level - 1].forward == null)
				this.level--;

			this.length--;
		}

		/**
		 * 从 skiplist 中删除和给定 obj 以及给定 score 匹配的元素
		 * <p/>
		 * T_worst = O(N), T_average = O(log N)
		 *
		 * @return 1=success 0=not found
		 */
		private int delete(long score, long obj) {
			int i;

			SkipListNode[] update = new SkipListNode[ZSKIPLIST_MAXLEVEL];

			SkipListNode x = this.header;
			// 遍历所有层，记录删除节点后需要被修改的节点到 update 数组
			for (i = this.level - 1; i >= 0; i--) {
				while (x.level[i].forward != null &&
						(x.level[i].forward.score < score ||
								(x.level[i].forward.score == score &&
										x.level[i].forward.obj < obj)))
					x = x.level[i].forward;
				update[i] = x;
			}
			// 因为多个不同的 member 可能有相同的 score 
			// 所以要确保 x 的 member 和 score 都匹配时，才进行删除
			x = x.level[0].forward;
			if (x != null && score == x.score && x.obj == obj) {
				this.deleteNode(x, update);
				return 1;
			} else {
				return 0;
			}
		}

		private static boolean keyGteMin(long key, RangeSpec spec) {
			return spec.minex ? (key > spec.min) : (key >= spec.min);
		}

		private static boolean keyLteMax(long key, RangeSpec spec) {
			return spec.maxex ? (key < spec.max) : (key <= spec.max);
		}

		/**
		 * 检查 skiplist 中的元素是否在给定范围之内
		 * <p/>
		 * T = O(1)
		 */
		private boolean isInRange(RangeSpec range) {
			SkipListNode x;

			// range 为空
			if (range.min > range.max ||
					(range.min == range.max && (range.minex || range.maxex)))
				return false;

			// 如果 skiplist 的最大节点的 score 比范围的最小值要小
			// 那么 skiplist 不在范围之内
			x = this.tail;
			if (x == null || !keyGteMin(x.score, range))
				return false;

			// 如果 skiplist 的最小节点的 score 比范围的最大值要大
			// 那么 skiplist 不在范围之内
			x = this.header.level[0].forward;
			if (x == null || !keyLteMax(x.score, range))
				return false;

			// 在范围内
			return true;
		}

		/**
		 * 找到跳跃表中第一个符合给定范围的元素
		 * <p/>
		 * T_worst = O(N) , T_average = O(log N)
		 */
		private SkipListNode firstInRange(RangeSpec range) {
			SkipListNode x;
			int i;

			if (!isInRange(range)) return null;

			// 找到第一个 score 值大于给定范围最小值的节点
			// O(N)
			x = this.header;
			for (i = this.level - 1; i >= 0; i--) {
				while (x.level[i].forward != null &&
						!keyGteMin(x.level[i].forward.score, range))
					x = x.level[i].forward;
			}

			x = x.level[0].forward;

			// O(1)
			if (!keyLteMax(x.score, range)) return null;
			return x;
		}

		/**
		 * 找到跳跃表中最后一个符合给定范围的元素
		 * <p/>
		 * T_worst = O(N) , T_average = O(log N)
		 */
		private SkipListNode lastInRange(RangeSpec range) {
			SkipListNode x;
			int i;

			if (!isInRange(range)) return null;

			// O(N)
			x = this.header;
			for (i = this.level - 1; i >= 0; i--) {
				while (x.level[i].forward != null &&
						keyLteMax(x.level[i].forward.score, range))
					x = x.level[i].forward;
			}

			if (!keyGteMin(x.score, range)) return null;
			return x;
		}


		/**
		 * 返回目标元素在有序集中的 rank
		 * <p/>
		 * 如果元素不存在于有序集，那么返回 0 。
		 * <p/>
		 * T_worst = O(N) , T_average = O(log N)
		 */
		private int getRank(long score, long obj) {
			SkipListNode x;
			int rank = 0;
			int i;

			x = this.header;
			// 遍历 skiplist ，并累积沿途的 span 到 rank ，找到目标元素时返回 rank
			// O(N)
			for (i = this.level - 1; i >= 0; i--) {
				while (x.level[i].forward != null &&
						(x.level[i].forward.score < score ||
								(x.level[i].forward.score == score &&
										x.level[i].forward.obj <= obj))) {
					// 累积
					rank += x.level[i].span;
					// 前进
					x = x.level[i].forward;
				}

				// 找到目标元素
				if (x.obj != null && x.obj == obj) {
					return rank;
				}
			}
			return 0;
		}

		/**
		 * 根据给定的 rank 查找元素
		 * <p/>
		 * T = O(N)
		 */
		private SkipListNode getElementByRank(int rank) {
			SkipListNode x;
			int traversed = 0;
			int i;

			// 沿着指针前进，直到累积的步数 traversed 等于 rank 为止
			// O(N)
			x = this.header;
			for (i = this.level - 1; i >= 0; i--) {
				while (x.level[i].forward != null && (traversed + x.level[i].span) <= rank) {
					traversed += x.level[i].span;
					x = x.level[i].forward;
				}
				if (traversed == rank) {
					return x;
				}
			}

			// 没找到
			return null;
		}

	}


	private static final int ZSKIPLIST_MAXLEVEL = 32; /* Should be enough for 2^32 elements */
	private static final float ZSKIPLIST_P = 0.25f;

	private SkipList list = new SkipList();
	private Map<Long, Long> dict = new HashMap<>();

	/**
	 * 清理这个 SortedSet
	 */
	public void clear() {
		synchronized (this) {
			this.list = new SkipList();
			this.dict.clear();
		}
	}

	/**
	 * 删除给定范围内的 score 的元素。
	 * <p/>
	 * T = O(N^2)
	 */
	private int deleteRangeByScore(RangeSpec range) {
		SkipListNode[] update = new SkipListNode[ZSKIPLIST_MAXLEVEL];
		SkipListNode x;
		int removed = 0;
		int i;

		// 记录沿途的节点
		// O(N)
		x = this.list.header;
		for (i = this.list.level - 1; i >= 0; i--) {
			while (x.level[i].forward != null && (range.minex ?
					x.level[i].forward.score <= range.min :
					x.level[i].forward.score < range.min))
				x = x.level[i].forward;
			update[i] = x;
		}

		x = x.level[0].forward;

		// 一直向右删除，直到到达 range 的底为止
		// O(N^2)
		while (x != null && (range.maxex ? x.score < range.max : x.score <= range.max)) {
			// 保存后继指针
			SkipListNode next = x.level[0].forward;
			// 在跳跃表中删除, O(N)
			this.list.deleteNode(x, update);
			// 在字典中删除，O(1)
			this.dict.remove(x.obj);

			removed++;

			x = next;
		}

		return removed;
	}

	/**
	 * 删除给定排序范围内的所有节点
	 * <p/>
	 * T = O(N^2)
	 */
	private int deleteRangeByRank(int start, int end) {
		SkipListNode[] update = new SkipListNode[ZSKIPLIST_MAXLEVEL];
		SkipListNode x;
		int traversed = 0, removed = 0;
		int i;

		// 通过计算 rank ，移动到删除开始的地方
		// O(N)
		x = this.list.header;
		for (i = this.list.level - 1; i >= 0; i--) {
			while (x.level[i].forward != null && (traversed + x.level[i].span) < start) {
				traversed += x.level[i].span;
				x = x.level[i].forward;
			}
			update[i] = x;
		}

		// 算上 start 节点
		traversed++;
		// 从 start 开始，删除直到到达索引 end ，或者末尾
		// O(N^2)
		x = x.level[0].forward;
		while (x != null && traversed <= end) {
			// 保存后一节点的指针
			SkipListNode next = x.level[0].forward;
			// 删除 skiplist 节点, O(N)
			this.list.deleteNode(x, update);
			// 删除 dict 节点, O(1)
			this.dict.remove(x.obj);

			removed++;
			traversed++;
			x = next;
		}
		return removed;
	}

	private String debugString() {
		StringBuilder sb = new StringBuilder();
		for (int i = this.list.level - 1; i >= 0; i--) {
			SkipListNode node = this.list.header;
			sb.append("level ").append(i).append(":");
			while (node.level[i].forward != null) {
				node = node.level[i].forward;
				sb.append("[k=").append(node.obj).append(":v=").append(node.score).append("]");
			}
			sb.append("\n");
		}

		return sb.toString();
	}



	/*-----------------------------------------------------------------------------
	 * sorted set API
	 *----------------------------------------------------------------------------*/

	public int size() {
		return this.dict.size();
	}

	/**
	 *
	 * @param key
	 * @return null if not found
	 */
	public Long getScore(long key){
		return this.dict.get(key);
	}

	/**
	 * 添加，会自动合并重复的key
	 *
	 * @param score
	 * @param key
	 */
	public void add(long score, long key) {
		synchronized (this) {
			if (this.dict.containsKey(key)) {
				this.list.delete(this.dict.get(key), key);
			}
			this.dict.put(key, score);
			this.list.insert(score, key);
		}
	}

	/**
	 * 批量添加，会自动合并重复的key
	 *
	 * @param recordObjects RecordObject::rank不会自动更新
	 */
	public void addAdll(RecordObject[] recordObjects) {
		synchronized (this) {
			for (RecordObject recordObject : recordObjects) {
				long score = recordObject.getScore();
				long key = recordObject.getKey();
				if (this.dict.containsKey(key)) {
					this.list.delete(this.dict.get(key), key);
				}
				this.dict.put(key, score);
				this.list.insert(score, key);
			}
		}
	}

	/**
	 * 批量添加，会自动合并重复的key
	 *
	 * @param recordObjects RecordObject::rank不会自动更新
	 */
	public void addAdll(Collection<RecordObject> recordObjects) {
		synchronized (this) {
			for (RecordObject recordObject : recordObjects) {
				long score = recordObject.getScore();
				long key = recordObject.getKey();
				if (this.dict.containsKey(key)) {
					this.list.delete(score, key);
				}
				this.dict.put(key, score);
				this.list.insert(score, key);
			}
		}
	}

	/**
	 * 删除一个值
	 *
	 * @param key
	 */
	public void remove(long key) {
		synchronized (this) {
			if (this.dict.containsKey(key)) {
				Long score = this.dict.remove(key);
				this.list.delete(score, key);
			}
		}
	}

	/**
	 * 通过分数范围删除
	 */
	public void removeByScore(RangeSpec range) {
		synchronized (this) {
			this.deleteRangeByScore(range);
		}
	}

	/**
	 * 通过分数范围删除
	 */
	public void removeByRank(int start, int end, boolean reverse) {

		synchronized (this) {
			if(reverse) {
				int size = this.size();
				this.deleteRangeByRank(size + 1 - end, size + 1 - start);
			} else {
				this.deleteRangeByRank(start, end);
			}
		}
	}

	/**
	 * 获取排名
	 *
	 * @param key
	 * @param reverse true=从大到小 false=从小到大
	 * @return
	 */
	public int rank(long key, boolean reverse) {
		synchronized (this) {
			if (this.dict.containsKey(key)) {
				Long score = this.dict.get(key);
				return reverse
						? this.dict.size() + 1 - this.list.getRank(score, key)
						: this.list.getRank(score, key);
			}
			return -1;
		}
	}

	/**
	 * 获得排名第x的对象
	 *
	 * @param rank
	 * @param reverse true=从大到小 false=从小到大
	 * @return
	 */
	public RecordObject getByRank(int rank, boolean reverse) {
		int realRank = reverse ? this.dict.size() + 1 - rank : rank;

		synchronized (this) {

			SkipListNode res = this.list.getElementByRank(realRank);
			if (res == null || res.obj == null) {
				return null;
			}
			RecordObject recordObject = res.toRecordObject();
			recordObject.setRank(rank);

			return recordObject;
		}
	}

	/**
	 * 通过排名获取一段
	 *
	 * @param rankBegin 低排名
	 * @param rankEnd   高排名
	 * @param reverse   true=从大到小 false=从小到大
	 * @return
	 */
	public List<RecordObject> getRangeByRank(int rankBegin, int rankEnd, boolean reverse) {
		if (rankBegin > rankEnd) {
			return null;
		}
		int size = this.dict.size();

		rankBegin = rankBegin < 1 ? 1 : rankBegin;
		rankEnd = rankEnd > size ? size : rankEnd;

		int realRankBegin = reverse ? size + 1 - rankEnd : rankBegin;

		int i = 0;
		int r = rankEnd - rankBegin;

		int rank = realRankBegin;

		List<RecordObject> ls = new LinkedList<>();

		synchronized (this) {
			SkipListNode node = this.list.getElementByRank(realRankBegin);
			if (node == null || node.obj == null) {
				return ls;
			}
			RecordObject ro = node.toRecordObject();
			ro.setRank(reverse ? size + 1 - rank : rank);
			ls.add(ro);

			while (i++ < r && node.level[0].forward != null) {
				node = node.level[0].forward;
				ro = node.toRecordObject();
				rank++;
				ro.setRank(reverse ? size + 1 - rank : rank);
				ls.add(ro);
			}

		}

		if(reverse) {
			Collections.reverse(ls);
		}

		return ls;
	}

	/**
	 * 通过分数获取一段 双闭区间
	 *
	 * @param scoreBegin 低分
	 * @param scoreEnd   高分
	 * @param reverse   true=从大到小 false=从小到大
	 * @return
	 */
	public List<RecordObject> getRangeByScore(int scoreBegin, int scoreEnd, boolean reverse) {
		if (scoreBegin > scoreEnd) {
			return null;
		}

		int size = this.dict.size();

		List<RecordObject> ls = new LinkedList<>();

		synchronized (this) {
			SkipListNode node = this.list.firstInRange(new RangeSpec(scoreBegin, scoreEnd));
			if (node == null || node.obj == null) {
				return ls;
			}

			int r = this.list.getRank(node.score, node.obj);

			RecordObject ro = node.toRecordObject();
			ro.setRank(reverse ? size + 1 - r : r);
			ls.add(ro);

			while (node.level[0].forward != null && node.level[0].forward.score <= scoreEnd) {
				node = node.level[0].forward;
				ro = node.toRecordObject();
				r++;
				ro.setRank(reverse ? size + 1 - r : r);
				ls.add(ro);
			}

		}

		if(reverse) {
			Collections.reverse(ls);
		}

		return ls;
	}

}
