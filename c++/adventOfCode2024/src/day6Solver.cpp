#include <cassert>
#include <iostream>
#include <map>
#include <set>
#include <solver.h>
#include <string>
#include <utility>

#include "day6Solver.h"

using namespace std;

enum Direction {UP, RIGHT, DOWN, LEFT};

struct Guard {
  int m_row;
  int m_col;
  Direction m_direction;
  int m_rowMove;
  int m_colMove;
  map<coordinate, set<Direction>> m_visits;

  void setStart(int row, int col) {
      m_row = row;
    m_col = col;
    m_visits.clear();
	m_direction = UP;
    m_rowMove = -1;
    m_colMove = 0;
  }

  bool move(size_t rowLimit, size_t colLimit, set<coordinate>& blocks) {
    while (m_row >= 0 && m_row < rowLimit && m_col >= 0 && m_col < colLimit) {
		auto coord = make_pair(m_row, m_col);
		auto fnd = m_visits.find(coord);
		if (fnd != m_visits.end() ) {
			if ( fnd->second.find(m_direction) != fnd->second.end() ) {
				return true;
			}
			fnd->second.insert(m_direction);
		} else {
			m_visits[ coord ] = { m_direction };
		}
		int blocked = 0;
		for ( int i = 0; i < 4; ++i ) {
			auto fnd = blocks.find(make_pair(m_row + m_rowMove, m_col + m_colMove));

			if ( fnd == blocks.end() ) {
				break;
			}
			++blocked;
			turnRight();
		}
      m_row += m_rowMove;
      m_col += m_colMove;
    }
	return false;
  }

  void turnRight() {
	  switch ( m_direction ) {
	  case UP:
		  m_rowMove = 0;
		  m_colMove = 1;
		  m_direction = RIGHT;
		  break;
	  case RIGHT:
		  m_rowMove = 1;
		  m_colMove = 0;
		  m_direction = DOWN;
		  break;
	  case DOWN:
		  m_rowMove = 0;
		  m_colMove = -1;
		  m_direction = LEFT;
		  break;
	  case LEFT:
		  m_rowMove = -1;
		  m_colMove = 0;
		  m_direction = UP;
		  break;
	  }
  }
};

static Guard s_guard;

day6Solver::day6Solver(const string& testFile) : solver(testFile), m_rowCount(0) {
}

void day6Solver::loadData(const string& line) {
  assert(m_blocks.empty() || line.size() == m_colCount);
  size_t offset = 0;
  size_t pos;

  while ((pos = line.find("#", offset)) != string::npos) {
    m_blocks.insert(make_pair((int)m_rowCount, (int)pos));
    offset = pos + 1;
  }
  pos = line.find("^");
  if (pos != string::npos) {
    s_guard.setStart((int)m_rowCount, (int)pos);
  }
  ++m_rowCount;
  m_colCount = line.size();
}

void day6Solver::clearData() {
  m_blocks.clear();
  m_rowCount = 0;
}

solveResult day6Solver::compute() {
  s_guard.move(m_rowCount, m_colCount, m_blocks);
  return  (long)s_guard.m_visits.size();
}

solveResult day6Solver::compute2() {
    long t = 0;
	int startRow = s_guard.m_row;
	int startCol = s_guard.m_col;
	s_guard.move(m_rowCount, m_colCount, m_blocks);

	set<coordinate> path;
	for ( auto& visited : s_guard.m_visits ) {
		path.insert(visited.first);
	}

	for ( auto& newBlock : path ) {
		m_blocks.insert(newBlock);
		s_guard.setStart(startRow, startCol);
		if ( s_guard.move(m_rowCount, m_colCount, m_blocks) ) {
			++t;
		}
		m_blocks.erase(newBlock);
	}

    return t;
}

void day6Solver::loadTestData() {
  clearData();
    loadData("....#.....");
    loadData(".........#");
    loadData("..........");
    loadData("..#.......");
    loadData(".......#..");
    loadData("..........");
    loadData(".#..^.....");
    loadData("........#.");
    loadData("#.........");
    loadData("......#...");
}
