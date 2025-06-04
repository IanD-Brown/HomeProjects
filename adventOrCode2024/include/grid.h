#pragma once

#include <functional>
#include <map>
#include <set>
#include <vector>

enum Direction { NORTH, SOUTH, EAST, WEST, NONE };
using Step = std::pair<size_t, Direction>;

static const char WALL('#');

static const Direction ALL_DIRECTIONS[] = {NORTH, SOUTH, EAST, WEST};

static Direction opposite(const Direction &from) {
	switch (from) {
	case NORTH:
		return SOUTH;
	case SOUTH:
		return NORTH;
	case EAST:
		return WEST;
	case WEST:
		return EAST;
	}
	return NONE;
}

static const char *ToString(const Direction &v) {
	switch(v) {
	case NORTH:
		return "N";
	case SOUTH:
		return "S";
	case EAST:
		return "E";
	case WEST:
		return "W";
	}
	return NULL;
}

template <typename K> struct Grid {
	using keyFactory = std::function<K(size_t, Direction)>;
	using cellMoveCost = std::function<solveResult(const std::map<K, solveResult> &, const K &)>;
	const std::vector<std::string>& m_data;
	const size_t m_rowCount;
	const size_t m_colCount;
	std::map<K, solveResult> m_cellCost;
	keyFactory m_keyFactory;
	cellMoveCost m_cellMoveCost;
	size_t m_start;

	Grid(const std::vector<std::string> &data,
		 keyFactory p_keyFactory,
		 cellMoveCost p_cellMoveCost) :
		m_start(0),
		m_data(data),
		m_rowCount(data.size()),
		m_colCount(data[0].size()),
		m_keyFactory(p_keyFactory),
		m_cellMoveCost(p_cellMoveCost) {}

	size_t index(size_t row, size_t col) const {
		return row * m_colCount + col;
	}

	size_t size() const { 
		return m_rowCount * m_colCount;
	}

	size_t row(size_t ravelIndex) const {
		return (ravelIndex - col(ravelIndex)) / m_colCount;
	}

	size_t col(size_t ravelIndex) const {
		return ravelIndex % m_colCount;
	}

	size_t move(size_t from, const Direction &direction) const {
		switch (direction) {
		case NORTH:
			return index(row(from) - 1, col(from));
		case SOUTH:
			return index(row(from) + 1, col(from));
		case EAST:
			return index(row(from), col(from) + 1);
		case WEST:
			return index(row(from), col(from) - 1);
		}
		return from;
	}

	bool isPath(size_t row, size_t col, Direction move) const {
		switch (move) {
		case NORTH:
			return row > 0 && m_data[row - 1][col] != WALL;
		case SOUTH:
			return row + 1 < m_rowCount && m_data[row + 1][col] != WALL;
		case EAST:
			return col + 1 < m_colCount && m_data[row][col + 1] != WALL;
		case WEST:
			return col > 0 && m_data[row][col - 1] != WALL;
		}
		return m_data[row][col] != WALL;
	}

	bool isPath(size_t from, Direction move) const {
		return isPath(row(from), col(from), move);
	}
	
	void calc(size_t start, bool isEnd) {
		m_cellCost.clear();
		m_start = start;
		m_cellCost[m_keyFactory(start, EAST)] = 0;
		if (isEnd)
			m_cellCost[m_keyFactory(start, NORTH)] = 0;

		std::set<Step> processing({make_pair(start, NONE)});

		while (!processing.empty()) {
			set<Step> next;
			for (const Step &p : processing) {
				calc(next, p);
			}
			processing = next;
		}
	}

	void calc(std::set<Step> &next, Step from) {
		Direction blocked(opposite(from.second));
		for (const Direction &d : ALL_DIRECTIONS) {
			if (d != blocked && isPath(from.first, d)) {
				bool horizontal(d == EAST || d == WEST);
				solveResult stepCost(
					m_cellMoveCost(m_cellCost, m_keyFactory(from.first, d)));

				size_t nextCell(move(from.first, d));
				K nextCostKey(m_keyFactory(nextCell, d));
				std::map<K, solveResult>::iterator nextCost(m_cellCost.find(nextCostKey));
				if (nextCost != m_cellCost.end()) {
					// existing
					if (nextCost->second > stepCost) {
						nextCost->second = stepCost;
						next.insert(make_pair(nextCell, d));
					}
				} else {
					m_cellCost[nextCostKey] = stepCost;
					next.insert(make_pair(nextCell, d));
				}
			}
		}
	}

};

