#pragma once

#include "solver.h"
#include <string>
#include <vector>

class day25Solver : public solver {
private:
  std::vector<std::vector<size_t>> m_locks;
  std::vector<std::vector<size_t>> m_keys;
  enum State {EMPTY, LOCK, KEY} m_state;
  std::vector<size_t> m_working;
  size_t m_height;

  virtual void loadFromFile(const std::string &file);

public:
	day25Solver(const std::string& testFile);

    virtual solveResult compute();

    virtual void clearData();

    virtual void loadTestData();

    virtual void loadData(const std::string& line);
};